package com.naturalist.plants.management;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.test.context.TestGraph;

/**
 * External test context of the management sub-context: a facade over a fresh {@link TestGraph}, which
 * builds the package's query implementation and repository mock by reflection under plants-core's
 * {@code PlantProgramTestContextInternal}. The wiring lives there once.
 */
public final class PlantProgramTestContext {

    private PlantProgramTestContext() {
    }

    public static PlantProgramQuery createQuery(NaturalistDatabase db) {
        return TestGraph.create(db).get(PlantProgramQuery.class);
    }
}
