package com.naturalist.plants.heritage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.ObjectGraph;

/**
 * External test context of the heritage sub-context: a facade over a fresh {@link ObjectGraph}, which
 * builds the package's query implementation and repository mock by reflection under plants-core's
 * {@code SeedLineageTestContextInternal}. The wiring lives there once.
 */
public final class SeedLineageTestContext {

    private SeedLineageTestContext() {
    }

    public static SeedLineageQuery createQuery(NaturalistDatabase db) {
        return ObjectGraph.create(db).get(SeedLineageQuery.class);
    }
}
