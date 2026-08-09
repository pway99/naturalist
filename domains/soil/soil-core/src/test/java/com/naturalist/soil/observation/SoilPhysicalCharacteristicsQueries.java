package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;

/**
 * Test-only wiring seam exposing a {@link SoilPhysicalCharacteristicsQuery} from a
 * {@link NaturalistDatabase}, mirroring {@link NutrientReadingQueries}.
 */
public final class SoilPhysicalCharacteristicsQueries {

    private SoilPhysicalCharacteristicsQueries() {
    }

    public static SoilPhysicalCharacteristicsQuery create(NaturalistDatabase database) {
        return new SoilPhysicalCharacteristicsQueryImpl(
                new SoilPhysicalCharacteristicsEntityRepositoryMock(database));
    }
}
