package com.naturalist.plants.cultivar;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.TestGraph;

/**
 * External test context of the cultivar sub-context: a facade over a fresh {@link TestGraph}, which
 * builds the package's query implementation and repository mock by reflection under plants-core's
 * {@code CultivarTestContextInternal}. The wiring lives there once.
 */
public final class CultivarTestContext {

    private CultivarTestContext() {
    }

    public static CultivarQuery createQuery(NaturalistDatabase db) {
        return TestGraph.create(db).get(CultivarQuery.class);
    }
}
