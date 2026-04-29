package com.naturalist.atlas;

import java.util.Arrays;
import java.util.List;

/**
 * Composition-root entry point for constructing an {@link Atlas}. Each app
 * collects the {@link AtlasContribution}s of the domains it includes and
 * passes them here.
 * <p>
 * Per the plan's "Per-app composition, not a shared registry module" rule, no
 * module under {@code domains/} or {@code kernels/} fans in to every domain
 * to assemble a global {@code Atlas}; that fan-in lives in each app's own
 * composition root. {@code AtlasAssembly} keeps that wiring trivially
 * declarative — a single static call.
 *
 * <h2>Why a static factory and not a builder</h2>
 * Apps in this codebase wire their composition root by construction, not by
 * stepwise mutation. A static {@code from(...)} keeps the call site short
 * and matches the {@code <Domain>QueryFactory.from(...)} idiom used
 * elsewhere in the kernel. If a future need calls for incremental
 * registration (e.g. a hot-reload mode) a builder can be added without
 * breaking the existing call sites.
 */
public final class AtlasAssembly {

    private AtlasAssembly() {
        // factory only
    }

    /**
     * Assemble an {@link Atlas} from the given contributions, in the order
     * supplied. Order does not affect correctness — a duplicate surface form
     * pointing at different targets is rejected regardless of which
     * contribution emits it first.
     */
    public static Atlas from(AtlasContribution... contributions) {
        return from(Arrays.asList(contributions));
    }

    /**
     * Assemble an {@link Atlas} from the given contributions. The list is
     * defensively iterated — the resulting {@link Atlas} does not retain a
     * reference to it.
     */
    public static Atlas from(List<AtlasContribution> contributions) {
        return new DefaultAtlas(contributions);
    }
}
