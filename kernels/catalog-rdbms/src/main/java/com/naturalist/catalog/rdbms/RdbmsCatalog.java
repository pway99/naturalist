package com.naturalist.catalog.rdbms;

import com.naturalist.catalog.*;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.Resilient;

import java.util.*;
import java.util.function.Function;

/**
 * Postgres-backed {@link Catalog}. Forward search and slug resolution delegate
 * to {@link CatalogSearchMapper} against the {@code catalog_search_token} view;
 * inverse back-reference routing delegates to the storage-independent
 * {@link ReferenceRouting} shared with the in-memory adapter.
 * <p>
 * Package-private — consumers obtain an instance via {@link RdbmsCatalogAssembly}.
 */
final class RdbmsCatalog implements Catalog {

    private static final Observer observer = Observer.forClass(RdbmsCatalog.class);

    private final CatalogSearchMapper mapper;
    private final Map<String, DomainId> domainsBySlug;
    private final Map<String, Function<String, EntityName>> reconstructors;
    private final ReferenceRouting routing;

    RdbmsCatalog(CatalogSearchMapper mapper,
                 Map<String, DomainId> domainsBySlug,
                 Map<String, Function<String, EntityName>> reconstructors,
                 ReferenceRouting routing) {
        this.mapper = mapper;
        this.domainsBySlug = Map.copyOf(domainsBySlug);
        this.reconstructors = Map.copyOf(reconstructors);
        this.routing = routing;
    }

    @Override
    public SearchResults search(String text) {
        if (text == null || text.isBlank() || text.trim().length() < 2) return SearchResults.empty();
        String q = text.trim();
        List<CatalogTokenRow> rows = mapper.search(q);

        Map<EntityRef, MatchKind> bestKind = new LinkedHashMap<>();
        Map<EntityRef, String> bestToken = new LinkedHashMap<>();
        Map<EntityRef, Double> bestSim = new LinkedHashMap<>();
        for (CatalogTokenRow row : rows) {
            EntityRef ref = toRef(row);
            if (ref == null) continue;
            MatchKind kind = MatchKind.valueOf(row.kind);
            MatchKind prev = bestKind.get(ref);
            if (prev == null || kind.ordinal() < prev.ordinal()) {
                bestKind.put(ref, kind);
                bestToken.put(ref, row.matchedToken);
                bestSim.put(ref, row.sim == null ? 0.0 : row.sim);
            }
        }

        if (bestKind.isEmpty()) {
            observer.observation(new UnresolvedSearchObservation(q.toLowerCase())).observe(Level.INFO);
            return SearchResults.empty();
        }

        List<SearchHit> hits = bestKind.entrySet().stream()
                .map(e -> new SearchHit(e.getKey(), bestToken.get(e.getKey()), e.getValue()))
                .sorted(Comparator
                        .comparingInt((SearchHit h) -> h.kind().ordinal())
                        .thenComparing(h -> -bestSim.getOrDefault(h.target(), 0.0))
                        .thenComparing(h -> h.target().name().value()))
                .toList();
        return SearchResults.of(hits);
    }

    @Override
    public Optional<EntityRef> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        return Optional.ofNullable(mapper.findBySlug(slug.trim())).map(this::toRef);
    }

    @Override
    public Set<DomainId> domainsReferencing(Class<? extends EntityName> referenceType) {
        return routing.domainsReferencing(referenceType);
    }

    @Override
    @Resilient(name = ReferenceRouting.CATALOG_FANOUT)
    public Map<DomainId, List<EntityRef>> findReferencesTo(EntityName target) {
        return routing.findReferencesTo(target);
    }

    private EntityRef toRef(CatalogTokenRow row) {
        DomainId domain = domainsBySlug.get(row.domain);
        Function<String, EntityName> fn = reconstructors.get(row.entityType);
        if (domain == null || fn == null) {
            observer.arguments("toRef", i -> i
                            .notNull(domain, "domain[" + row.domain + "]")
                            .notNull(fn, "reconstructor[" + row.entityType + "]"))
                    .observe(Level.WARN);
            return null; // defensive; startup validation should prevent this
        }
        return new EntityRef(domain, fn.apply(row.slug));
    }
}
