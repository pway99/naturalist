package com.naturalist.plants.heritage;

import com.naturalist.test.query.nofanout.SelectTrace;
import com.naturalist.data.NaturalistDatabase;

/**
 * Same-package assembly seam for the heritage sub-context. Lives here (not folded into
 * {@code PlantsTestContext}) because PlantSeedLineageQueryImpl and PlantSeedLineageRepositoryMock are
 * package-private in this package; the root-package PlantsTestContext cannot reach them.
 */
public final class SeedLineageTestContext {

    private SeedLineageTestContext() {
    }

    public static SeedLineageQuery createQuery(NaturalistDatabase db) {
        return SelectTrace.traced(SeedLineageQuery.class, new PlantSeedLineageQueryImpl(new PlantSeedLineageRepositoryMock(db)));
    }
}
