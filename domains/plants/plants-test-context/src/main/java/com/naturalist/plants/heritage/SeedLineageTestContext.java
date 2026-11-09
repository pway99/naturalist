package com.naturalist.plants.heritage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.TestGraph;

/**
 * External test context of the heritage sub-context: a facade over a fresh {@link TestGraph}, which
 * builds the package's query implementation and repository mock by reflection under plants-core's
 * {@code SeedLineageTestContextInternal}. The wiring lives there once.
 */
public final class SeedLineageTestContext {

    private SeedLineageTestContext() {
    }

    public static SeedLineageQuery createQuery(NaturalistDatabase db) {
        return TestGraph.create(db).get(SeedLineageQuery.class);
    }
}
