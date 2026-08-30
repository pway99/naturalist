package com.naturalist.catalog.rdbms;

import com.naturalist.catalog.*;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.Resilience;

import java.util.*;
import java.util.function.Function;

/** Composition-root entry point for the Postgres-backed {@link Catalog}. */
public final class RdbmsCatalogAssembly {

    private static final Observer observer = Observer.forClass(RdbmsCatalogAssembly.class);

    private RdbmsCatalogAssembly() {}

    public static Catalog from(CatalogSearchMapper mapper,
                               List<DomainId> domains,
                               Map<String, Function<String, EntityName>> nameReconstructors,
                               List<EntityReferences<?>> providers,
                               Resilience resilience) {
        observer.arguments("from", i -> i
                        .notNull(mapper, "mapper")
                        .notNull(domains, "domains")
                        .notNull(nameReconstructors, "nameReconstructors")
                        .notNull(providers, "providers")
                        .notNull(resilience, "resilience"))
                .throwWhenInvalid();
        Map<String, DomainId> bySlug = new LinkedHashMap<>();
        for (DomainId d : domains) {
            DomainId prev = bySlug.putIfAbsent(d.value(), d);
            if (prev != null && !prev.equals(d)) {
                throw new IllegalArgumentException("Duplicate domain slug \"" + d.value() + "\"");
            }
        }
        validateRegistry(mapper, bySlug, nameReconstructors);
        return new RdbmsCatalog(mapper, bySlug, nameReconstructors,
                new ReferenceRouting(providers, resilience));
    }

    /** Fail fast: every (domain, entity_type) the view can emit must resolve in the registries. */
    private static void validateRegistry(CatalogSearchMapper mapper,
                                         Map<String, DomainId> domains,
                                         Map<String, Function<String, EntityName>> reconstructors) {
        for (CatalogTokenRow row : mapper.distinctDomainTypes()) {
            if (!domains.containsKey(row.domain)) {
                throw new IllegalStateException("catalog view emits domain '" + row.domain
                        + "' with no registered DomainId");
            }
            if (!reconstructors.containsKey(row.entityType)) {
                throw new IllegalStateException("catalog view emits entity_type '" + row.entityType
                        + "' with no registered EntityName reconstructor");
            }
        }
    }
}
