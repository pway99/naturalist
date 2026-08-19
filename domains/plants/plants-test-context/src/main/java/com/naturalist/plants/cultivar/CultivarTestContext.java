package com.naturalist.plants.cultivar;

import com.naturalist.data.NaturalistDatabase;

/**
 * Same-package assembly seam for the cultivar sub-context. Lives here (not folded into
 * {@code PlantsTestContext}) because PlantCultivarQueryImpl and PlantCultivarRepositoryMock are
 * package-private in this package; the root-package PlantsTestContext cannot reach them.
 */
public final class CultivarTestContext {

    private CultivarTestContext() {
    }

    public static CultivarQuery createQuery(NaturalistDatabase db) {
        return new PlantCultivarQueryImpl(new PlantCultivarRepositoryMock(db));
    }
}
