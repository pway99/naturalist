package com.naturalist.soil;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.soil.observation.LabAnalysisInfoQueries;
import com.naturalist.soil.observation.LabAnalysisInfoQuery;
import com.naturalist.soil.observation.NutrientReadingQueries;
import com.naturalist.soil.observation.NutrientReadingQuery;
import com.naturalist.soil.observation.ReportedOptimumQueries;
import com.naturalist.soil.observation.ReportedOptimumQuery;
import com.naturalist.soil.observation.ReportedRecommendationQueries;
import com.naturalist.soil.observation.ReportedRecommendationQuery;
import com.naturalist.soil.observation.SoilPhysicalCharacteristicsQueries;
import com.naturalist.soil.observation.SoilPhysicalCharacteristicsQuery;

/**
 * Full-graph test wiring for {@code soil-core} tests: constructs the entity queries (profile
 * header, analysis header, nutrient readings, physical characteristics), the aggregate factory,
 * and the {@link SoilProfileQuery} — mirroring the (deferred) module-level {@code SoilTestContext}
 * but living in core's own test classpath to avoid the Maven cycle. Goes away when Spring DI
 * replaces the manual composition.
 */
class SoilsTestContextInternal {

    private final SoilProfileQuery soilProfileQuery;

    private SoilsTestContextInternal(NaturalistDatabase db) {
        SoilProfileInfoQuery soilProfileInfoQuery =
                new SoilProfileInfoQueryImpl(new SoilProfileInfoEntityRepositoryMock(db));
        LabAnalysisInfoQuery labAnalysisInfoQuery = LabAnalysisInfoQueries.create(db);
        NutrientReadingQuery nutrientReadingQuery = NutrientReadingQueries.create(db);
        SoilPhysicalCharacteristicsQuery physicalCharacteristicsQuery =
                SoilPhysicalCharacteristicsQueries.create(db);
        ReportedOptimumQuery reportedOptimumQuery = ReportedOptimumQueries.create(db);
        ReportedRecommendationQuery reportedRecommendationQuery =
                ReportedRecommendationQueries.create(db);
        SoilProfileFactory factory = new SoilProfileFactory(
                soilProfileInfoQuery, labAnalysisInfoQuery, nutrientReadingQuery,
                physicalCharacteristicsQuery, reportedOptimumQuery, reportedRecommendationQuery);
        this.soilProfileQuery = new SoilProfileQueryImpl(factory);
    }

    static SoilsTestContextInternal create(NaturalistDatabase db) {
        return new SoilsTestContextInternal(db);
    }

    SoilProfileQuery soilProfileQuery() {
        return soilProfileQuery;
    }
}
