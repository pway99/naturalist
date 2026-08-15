package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;

/**
 * Wiring seam exposing a {@link ReportedOptimumQuery} from a {@link NaturalistDatabase}, mirroring
 * {@link NutrientReadingQueries}.
 */
public final class ReportedOptimumQueries {

    private ReportedOptimumQueries() {
    }

    public static ReportedOptimumQuery create(NaturalistDatabase database) {
        return new ReportedOptimumQueryImpl(new ReportedOptimumEntityRepositoryMock(database));
    }
}
