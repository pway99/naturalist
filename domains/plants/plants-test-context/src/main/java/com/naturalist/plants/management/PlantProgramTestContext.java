package com.naturalist.plants.management;

import com.naturalist.data.NaturalistDatabase;

/**
 * Same-package assembly seam for the management sub-context. Lives here (not folded into
 * {@code PlantsTestContext}) because PlantProgramQueryImpl and PlantProgramRepositoryMock are
 * package-private in this package; the root-package PlantsTestContext cannot reach them.
 */
public final class PlantProgramTestContext {

    private PlantProgramTestContext() {
    }

    public static PlantProgramQuery createQuery(NaturalistDatabase db) {
        return new PlantProgramQueryImpl(new PlantProgramRepositoryMock(db));
    }
}
