package com.naturalist.soil;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.soil.observation.LabAnalysisInfoQueries;
import com.naturalist.soil.observation.LabAnalysisInfoQuery;
import com.naturalist.soil.observation.NutrientReadingQueries;
import com.naturalist.soil.observation.NutrientReadingQuery;
import com.naturalist.soil.observation.SoilPhysicalCharacteristicsQueries;
import com.naturalist.soil.observation.SoilPhysicalCharacteristicsQuery;

/**
 * Pre-wired, in-memory read surface for the soil bounded context: assembles the entity queries and
 * the {@code SoilProfileFactory}, exposing the {@link SoilProfileQuery} (assembled profile) and the
 * {@link SoilProfileInfoQuery} (profile enumeration). Lives in package {@code com.naturalist.soil}
 * for split-package access to soil-core's package-private impls. Goes away when Spring DI replaces
 * the manual composition.
 */
public class SoilTestContext {

    private final SoilProfileInfoQuery soilProfileInfoQuery;
    private final SoilProfileQuery soilProfileQuery;

    private SoilTestContext(NaturalistDatabase db) {
        this.soilProfileInfoQuery =
                new SoilProfileInfoQueryImpl(new SoilProfileInfoEntityRepositoryMock(db));
        LabAnalysisInfoQuery labAnalysisInfoQuery = LabAnalysisInfoQueries.create(db);
        NutrientReadingQuery nutrientReadingQuery = NutrientReadingQueries.create(db);
        SoilPhysicalCharacteristicsQuery physicalCharacteristicsQuery =
                SoilPhysicalCharacteristicsQueries.create(db);
        SoilProfileFactory factory = new SoilProfileFactory(
                soilProfileInfoQuery, labAnalysisInfoQuery, nutrientReadingQuery, physicalCharacteristicsQuery);
        this.soilProfileQuery = new SoilProfileQueryImpl(factory);
    }

    public static SoilTestContext create(NaturalistDatabase db) {
        return new SoilTestContext(db);
    }

    public SoilProfileQuery soilProfileQuery() {
        return soilProfileQuery;
    }

    public SoilProfileInfoQuery soilProfileInfoQuery() {
        return soilProfileInfoQuery;
    }
}
