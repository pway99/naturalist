package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;

/**
 * Test-only wiring seam exposing a {@link NutrientReadingQuery} from a {@link NaturalistDatabase},
 * so full-graph wiring in the parent {@code com.naturalist.soil} package can assemble the
 * observation sub-context without reaching its package-private impls directly.
 */
public final class NutrientReadingQueries {

    private NutrientReadingQueries() {
    }

    public static NutrientReadingQuery create(NaturalistDatabase database) {
        return new NutrientReadingQueryImpl(new NutrientReadingEntityRepositoryMock(database));
    }
}
