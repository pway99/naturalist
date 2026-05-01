package com.naturalist.atlas.inmem;

import com.naturalist.atlas.Atlas;
import com.naturalist.atlas.AtlasContribution;
import com.naturalist.atlas.EntityReferences;

import java.util.Arrays;
import java.util.List;

/**
 * Composition-root entry point for constructing an in-memory {@link Atlas}.
 * Each app collects the {@link AtlasContribution}s and {@link EntityReferences}
 * providers of the domains it includes and passes them here.
 * <p>
 * Per the plan's "Per-app composition, not a shared registry module" rule, no
 * module under {@code domains/} or {@code kernels/} fans in to every domain
 * to assemble a global {@code Atlas}; that fan-in lives in each app's own
 * composition root. {@code AtlasAssembly} keeps that wiring trivially
 * declarative — a single static call.
 *
 * <h2>Adapter selection at the import site</h2>
 * The factory returns the kernel's in-memory adapter ({@link InMemoryAtlas}).
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
 * The varargs {@link #from(AtlasContribution...)} and single-list
 * {@link #from(List)} overloads remain for callers that wire only the
 * forward direction (the M2 surface). Callers that also wire inverse
 * providers use {@link #from(List, List)}.
 */
public final class AtlasAssembly {

    private AtlasAssembly() {
        // factory only
    }

    /**
     * Assemble a forward-only {@link Atlas} from the given contributions.
     * Equivalent to {@code from(Arrays.asList(contributions), List.of())}.
     */
    public static Atlas from(AtlasContribution... contributions) {
        return from(Arrays.asList(contributions), List.of());
    }

    /**
     * Assemble a forward-only {@link Atlas} from the given contributions.
     * Equivalent to {@code from(contributions, List.of())}.
     */
    public static Atlas from(List<AtlasContribution> contributions) {
        return from(contributions, List.of());
    }

    /**
     * Assemble an {@link Atlas} from forward-direction contributions and
     * inverse-direction providers. Either list may be empty. The lists are
     * defensively iterated — the resulting {@link Atlas} does not retain a
     * reference to either.
     * <p>
     * Order within each list is preserved: aliases are indexed in the order
     * supplied (a duplicate surface form pointing at different targets is
     * rejected regardless of which contribution emits it first), and
     * providers for the same {@code referenceType()} are concatenated in the
     * order supplied when {@link Atlas#findReferencesTo(com.naturalist.ddd.EntityName)}
     * groups results by domain.
     */
    public static Atlas from(List<AtlasContribution> contributions,
                             List<EntityReferences<?>> providers) {
        return new InMemoryAtlas(contributions, providers);
    }
}
