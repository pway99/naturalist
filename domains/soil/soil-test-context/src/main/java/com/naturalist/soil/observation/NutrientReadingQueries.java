package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;

/**
 * Wiring seam exposing a {@link NutrientReadingQuery} from a {@link NaturalistDatabase}, mirroring
 * {@link LabAnalysisInfoQueries}.
 */
public final class NutrientReadingQueries {

    private NutrientReadingQueries() {
    }

    public static NutrientReadingQuery create(NaturalistDatabase database) {
        return new NutrientReadingQueryImpl(new NutrientReadingEntityRepositoryMock(database));
    }
}
