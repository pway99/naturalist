package com.naturalist.catalog.inmem;

import com.naturalist.catalog.*;
import com.naturalist.catalog.CatalogContribution.SearchableEntity;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Resilient;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * In-memory {@link Catalog} assembled from a list of {@link CatalogContribution}s
 * (search direction) and a list of {@link EntityReferences} providers
 * (inverse direction). Suitable for catalog scales where the full token
 * index fits comfortably in heap (the Oak Vista deployment); Lucene-backed
 * or other persistent adapters would ship as sibling modules implementing
 * {@link Catalog} directly.
 *
 * <h2>Token index</h2>
 * Every contributed token is lowercased and split on whitespace and ASCII
 * punctuation, and each resulting term is mapped to the contributing
 * {@link EntityRef}s. The slug of each {@link SearchableEntity} is also
 * recorded as a slug-token so {@link MatchKind#EXACT_SLUG} can be reported
 * separately from {@link MatchKind#EXACT_TOKEN}. Multiple entities indexing
 * the same token is the normal case, not a conflict.
 *
 * <h2>Inverse-direction routing</h2>
 * Inverse providers are indexed by their {@code referenceType()}; the actual
 * fan-out runs live on each {@link Catalog#findReferencesTo(EntityName)} call.
 * Per the plan's "Registry shape: routing, not graph" decision, no inbound
 * reference set is materialised at startup.
 *
 * <h2>Argument validation</h2>
 * Null checks at the assembly boundary go through the kernel's
 * {@link Observer#arguments(String, java.util.function.Consumer)} pipeline.
 *
 * <h2>Visibility</h2>
 * The constructor is package-private. Consumers go through
 * {@link CatalogAssembly}.
 */
final class InMemoryCatalog implements Catalog {

    private static final Observer observer = Observer.forClass(InMemoryCatalog.class);

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s\\p{Punct}]+");

    private final Map<String, Set<EntityRef>> tokenIndex;
    private final Set<String> slugTokens;
    private final Map<String, EntityRef> refBySlug;
    private final ReferenceRouting routing;

    InMemoryCatalog(List<CatalogContribution> contributions,
                    List<EntityReferences<?>> providers,
                    Resilience resilience) {
        observer.arguments("constructor", i -> i
                        .notNull(contributions, "contributions")
                        .notNull(providers, "providers")
                        .notNull(resilience, "resilience"))
                .throwWhenInvalid();
        Map<String, Set<EntityRef>> index = new HashMap<>();
        Set<String> slugs = new LinkedHashSet<>();
        Map<String, EntityRef> bySlug = new HashMap<>();
        for (CatalogContribution contribution : contributions) {
            observer.arguments("constructor", i -> i.notNull(contribution, "contribution"))
                    .throwWhenInvalid();
            contribution.searchableEntities().forEach(entity -> {
                observer.arguments("constructor", i -> i.notNull(entity, "entity"))
                        .throwWhenInvalid();
                EntityRef target = entity.target();
                String slug = target.name().value().toLowerCase();
                slugs.add(slug);
                bySlug.putIfAbsent(slug, target);
                addAll(index, slug, target);
                entity.tokens().forEach(token -> tokenise(token).forEach(t -> addAll(index, t, target)));
            });
        }
        this.tokenIndex = freezeIndex(index);
        this.slugTokens = Set.copyOf(slugs);
        this.refBySlug = Map.copyOf(bySlug);
        for (EntityReferences<?> provider : providers) {
            observer.arguments("constructor", i -> i.notNull(provider, "provider"))
                    .throwWhenInvalid();
        }
        this.routing = new ReferenceRouting(providers, resilience);
    }

    @Override
    public Optional<EntityRef> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        return Optional.ofNullable(refBySlug.get(slug.trim().toLowerCase()));
    }

    private static void addAll(Map<String, Set<EntityRef>> index, String token, EntityRef target) {
        if (token == null || token.isBlank()) return;
        index.computeIfAbsent(token, k -> new LinkedHashSet<>()).add(target);
    }

    private static Stream<String> tokenise(String text) {
        if (text == null || text.isBlank()) return Stream.empty();
        // Length-1 fragments are dropped on both sides of the index. They're
        // signal-poor (the genus initial in "A. californica" is meaningful
        // only in context with the species) and they make every prose token
        // starting with a common letter a false positive — "not-a-thing"
        // should not match every entry whose abbreviated binomial happens
        // to start with "a".
        return TOKEN_SPLIT.splitAsStream(text.toLowerCase())
                .filter(s -> s.length() >= 2);
    }

    private static Map<String, Set<EntityRef>> freezeIndex(Map<String, Set<EntityRef>> index) {
        Map<String, Set<EntityRef>> immutable = new HashMap<>(index.size());
        index.forEach((k, v) -> immutable.put(k, Collections.unmodifiableSet(new LinkedHashSet<>(v))));
        return Collections.unmodifiableMap(immutable);
    }

    @Override
    public SearchResults search(String text) {
        if (text == null || text.isBlank()) return SearchResults.empty();
        List<String> queryTokens = tokenise(text).toList();
        if (queryTokens.isEmpty()) return SearchResults.empty();

        Map<EntityRef, MatchKind> bestKindByTarget = new LinkedHashMap<>();
        Map<EntityRef, String> matchedTokenByTarget = new LinkedHashMap<>();

        // Slug fast-path: the trimmed, lowercased input as a whole, matched
        // against the index. A reader who types "california-pipevine"
        // expects an EXACT_SLUG hit — without this pass the per-token
        // splitter would only see "california" and "pipevine" and the slug
        // would only surface as PREFIX.
        String normalized = text.trim().toLowerCase();
        recordExactMatches(normalized, bestKindByTarget, matchedTokenByTarget);

        for (String token : queryTokens) {
            recordExactMatches(token, bestKindByTarget, matchedTokenByTarget);
            recordPrefixMatches(token, bestKindByTarget, matchedTokenByTarget);
        }

        if (bestKindByTarget.isEmpty()) {
            // Firing site for M9-revised: instantiating the observation walks
            // its constraints through the Observer pipeline (no-op for valid
            // input). M9 wires the typed-event subscribers (Micrometer counter,
            // structured INFO log) onto the same construction site.
            UnresolvedSearchObservation miss = new UnresolvedSearchObservation(
                    text.trim().toLowerCase());
            observer.observation(miss).observe(Level.INFO);
            return SearchResults.empty();
        }

        List<SearchHit> hits = bestKindByTarget.entrySet().stream()
                .map(e -> new SearchHit(e.getKey(), matchedTokenByTarget.get(e.getKey()), e.getValue()))
                .sorted(Comparator
                        .comparingInt((SearchHit h) -> h.kind().ordinal())
                        .thenComparing(h -> h.target().name().value()))
                .toList();
        return SearchResults.of(hits);
    }

    private void recordExactMatches(String token,
                                    Map<EntityRef, MatchKind> bestKindByTarget,
                                    Map<EntityRef, String> matchedTokenByTarget) {
        Set<EntityRef> exact = tokenIndex.get(token);
        if (exact == null) return;
        MatchKind kind = slugTokens.contains(token) ? MatchKind.EXACT_SLUG : MatchKind.EXACT_TOKEN;
        for (EntityRef target : exact) {
            // EXACT_SLUG only applies when the matching token is the target's own slug —
            // a search for a genus token that happens to also be some other entity's
            // slug is still EXACT_TOKEN for the genus matches.
            MatchKind kindForTarget = (kind == MatchKind.EXACT_SLUG
                    && target.name().value().equalsIgnoreCase(token))
                    ? MatchKind.EXACT_SLUG : MatchKind.EXACT_TOKEN;
            promote(bestKindByTarget, matchedTokenByTarget, target, kindForTarget, token);
        }
    }

    private void recordPrefixMatches(String token,
                                     Map<EntityRef, MatchKind> bestKindByTarget,
                                     Map<EntityRef, String> matchedTokenByTarget) {
        if (token.length() < 2) return;
        for (Map.Entry<String, Set<EntityRef>> entry : tokenIndex.entrySet()) {
            String indexed = entry.getKey();
            if (indexed.equals(token) || !indexed.startsWith(token)) continue;
            for (EntityRef target : entry.getValue()) {
                promote(bestKindByTarget, matchedTokenByTarget, target, MatchKind.PREFIX, indexed);
            }
        }
    }

    private static void promote(Map<EntityRef, MatchKind> bestKindByTarget,
                                Map<EntityRef, String> matchedTokenByTarget,
                                EntityRef target, MatchKind kind, String token) {
        MatchKind previous = bestKindByTarget.get(target);
        if (previous == null || kind.ordinal() < previous.ordinal()) {
            bestKindByTarget.put(target, kind);
            matchedTokenByTarget.put(target, token);
        }
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
}
