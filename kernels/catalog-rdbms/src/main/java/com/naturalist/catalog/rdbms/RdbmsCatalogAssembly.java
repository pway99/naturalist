package com.naturalist.catalog.rdbms;

import com.naturalist.catalog.*;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.MethodObserver;
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
        MethodObserver m = observer.forMethod("from");
        m.arguments(i -> i
                .notNull(mapper, "mapper")
                .notNull(domains, "domains")
                .notNull(nameReconstructors, "nameReconstructors")
                .notNull(providers, "providers")
                .notNull(resilience, "resilience"))
                .throwWhenInvalid();
        Map<String, DomainId> bySlug = new LinkedHashMap<>();
        for (DomainId d : domains) {
            DomainId prev = bySlug.putIfAbsent(d.value(), d);
            m.arguments(i -> i
                    .isTrue(prev == null || prev.equals(d), "uniqueDomainSlug"))
                    .throwWhenInvalid();
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
            observer.state("validateRegistry", s -> s
                    .isTrue(domains.containsKey(row.domain), "registeredDomain")
                    .isTrue(reconstructors.containsKey(row.entityType), "registeredEntityType"))
                    .throwWhenInvalid();
        }
    }
}
