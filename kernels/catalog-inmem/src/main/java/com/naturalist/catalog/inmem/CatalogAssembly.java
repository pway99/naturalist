package com.naturalist.catalog.inmem;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.CatalogContribution;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityReferences;
import com.naturalist.resilience.Resilience;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Composition-root entry point for constructing an in-memory {@link Catalog}.
 * Each app collects the {@link CatalogContribution}s and {@link EntityReferences}
 * providers of the domains it includes and passes them here.
 * <p>
 * Per the plan's "Per-app composition, not a shared registry module" rule, no
 * module under {@code domains/} or {@code kernels/} fans in to every domain
 * to assemble a global {@code Catalog}; that fan-in lives in each app's own
 * composition root. {@code CatalogAssembly} keeps that wiring trivially
 * declarative — a single static call.
 *
 * <h2>Adapter selection at the import site</h2>
 * The factory returns the kernel's in-memory adapter ({@link InMemoryCatalog}).
 * A future Lucene-backed or persistent adapter would ship as its own sibling
 * module with its own assembly factory; choosing the adapter is therefore an
 * import-site decision, visible at every consumer.
 *
 * <h2>Why a static factory and not a builder</h2>
 * Apps in this codebase wire their composition root by construction, not by
 * stepwise mutation. A static {@code from(...)} keeps the call site short
 * and matches the {@code <Domain>QueryFactory.from(...)} idiom used
 * elsewhere in the kernel. If a future need calls for incremental
 * registration (e.g. a hot-reload mode) a builder can be added without
 * breaking the existing call sites.
 *
 * <h2>Forward-only and full overloads</h2>
 * The varargs {@link #from(CatalogContribution...)} and single-list
 * {@link #from(List)} overloads remain for callers that wire only the
 * forward direction (the M2 surface). Callers that also wire inverse
 * providers use {@link #from(List, List)}.
 *
 * <h2>Runtime-injected domain list</h2>
 * The {@link #from(List, List, List)} overload accepts the registered
 * {@link DomainId}s explicitly. Runtime adapters (e.g. {@code spring-runtime})
 * collect every {@code DomainId} bean and pass them in directly, so the
 * slug-uniqueness check covers domains whose contributions and providers
 * haven't been wired yet — a misnamed domain is rejected at startup
 * regardless of which sub-context happens to register a contribution first.
 */
public final class CatalogAssembly {

    private CatalogAssembly() {
        // factory only
    }

    /**
     * Assemble a forward-only {@link Catalog} from the given contributions.
     * Equivalent to {@code from(Arrays.asList(contributions), List.of())}.
     */
    public static Catalog from(CatalogContribution... contributions) {
        return from(Arrays.asList(contributions), List.of());
    }

    /**
     * Assemble a forward-only {@link Catalog} from the given contributions.
     * Equivalent to {@code from(contributions, List.of())}.
     */
    public static Catalog from(List<CatalogContribution> contributions) {
        return from(contributions, List.of());
    }

    /**
     * Assemble a {@link Catalog} from forward-direction contributions and
     * inverse-direction providers. Either list may be empty. The lists are
     * defensively iterated — the resulting {@link Catalog} does not retain a
     * reference to either.
     * <p>
     * Order within each list is preserved: aliases are indexed in the order
     * supplied (a duplicate surface form pointing at different targets is
     * rejected regardless of which contribution emits it first), and
     * providers for the same {@code referenceType()} are concatenated in the
     * order supplied when {@link Catalog#findReferencesTo(com.naturalist.ddd.EntityName)}
     * groups results by domain.
     */
    public static Catalog from(List<CatalogContribution> contributions,
                               List<EntityReferences<?>> providers) {
        return from(List.of(), contributions, providers);
    }

    /**
     * Assemble a {@link Catalog} from an explicit list of registered
     * {@link DomainId}s plus forward-direction contributions and
     * inverse-direction providers. Every list may be empty.
     * <p>
     * The {@code domains} list participates in slug-uniqueness validation
     * alongside the slugs reachable through {@code contributions} and
     * {@code providers}, so a domain registered without any contribution
     * still has its slug enforced.
     * <p>
     * The {@link Resilience} facade defaults to {@link Resilience#noOp()};
     * tests and pre-production composition roots that have not yet wired a
     * production adapter run inverse-direction fan-out unprotected. Apps
     * that have wired a production adapter use
     * {@link #from(List, List, List, Resilience)}.
     */
    public static Catalog from(List<DomainId> domains,
                               List<CatalogContribution> contributions,
                               List<EntityReferences<?>> providers) {
        return from(domains, contributions, providers, Resilience.noOp());
    }

    /**
     * Assemble a {@link Catalog} with a composition-root-supplied
     * {@link Resilience}. The catalog's inverse-direction fan-out wraps each
     * provider invocation with the {@code catalog.fanout} timeout and circuit
     * breaker resolved from {@code resilience}; an unconfigured name fails
     * fast at the first fan-out call (per the production adapter's contract).
     */
    public static Catalog from(List<DomainId> domains,
                               List<CatalogContribution> contributions,
                               List<EntityReferences<?>> providers,
                               Resilience resilience) {
        validateSlugUniqueness(domains, contributions, providers);
        return new InMemoryCatalog(contributions, providers, resilience);
    }

    /**
     * Enforces the open-{@link DomainId} invariant: the {@code source_domain}
     * metric tag's cardinality is bounded by what the composition root
     * registers, and registering two distinct {@link DomainId} instances that
     * share a {@code value()} would produce two domains masquerading under one
     * tag. Replaces the bound previously given by the sealed-interface shape.
     * <p>
     * Same-instance reuse across multiple contributions or providers is the
     * normal case (a domain typically ships one {@code DomainId} record and
     * shares it between its forward and inverse wiring) and is allowed.
     */
    private static void validateSlugUniqueness(List<DomainId> domains,
                                               List<CatalogContribution> contributions,
                                               List<EntityReferences<?>> providers) {
        Map<String, DomainId> bySlug = new LinkedHashMap<>();
        for (DomainId domain : domains) {
            recordSlug(bySlug, domain);
        }
        for (CatalogContribution contribution : contributions) {
            if (contribution == null) continue;
            recordSlug(bySlug, contribution.domain());
        }
        for (EntityReferences<?> provider : providers) {
            if (provider == null) continue;
            recordSlug(bySlug, provider.domain());
        }
    }

    private static void recordSlug(Map<String, DomainId> bySlug, DomainId domain) {
        if (domain == null) return;
        DomainId previous = bySlug.putIfAbsent(domain.value(), domain);
        if (previous != null && !previous.equals(domain)) {
            throw new IllegalArgumentException(
                    "Duplicate domain slug \"" + domain.value() + "\" registered by "
                            + previous.getClass().getName() + " and "
                            + domain.getClass().getName());
        }
    }
}
