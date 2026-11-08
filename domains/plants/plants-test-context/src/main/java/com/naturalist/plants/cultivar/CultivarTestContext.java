package com.naturalist.plants.cultivar;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.ObjectGraph;

/**
 * External test context of the cultivar sub-context: a facade over a fresh {@link ObjectGraph}, which
 * builds the package's query implementation and repository mock by reflection under plants-core's
 * {@code CultivarTestContextInternal}. The wiring lives there once.
 */
public final class CultivarTestContext {

    private CultivarTestContext() {
    }

    public static CultivarQuery createQuery(NaturalistDatabase db) {
        return ObjectGraph.create(db).get(CultivarQuery.class);
    }
}
