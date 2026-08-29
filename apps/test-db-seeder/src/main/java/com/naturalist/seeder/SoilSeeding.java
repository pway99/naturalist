package com.naturalist.seeder;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.soil.SoilProfileInfoSeeder;
import com.naturalist.soil.observation.LabAnalysisInfoSeeder;
import com.naturalist.soil.observation.NutrientReadingSeeder;
import com.naturalist.soil.observation.ReportedOptimumSeeder;
import com.naturalist.soil.observation.ReportedRecommendationSeeder;
import com.naturalist.soil.observation.SoilPhysicalCharacteristicsSeeder;

import javax.sql.DataSource;

/**
 * Seeds the soil domain. Cross-entity references are soft (no FK), so order is immaterial; profiles and
 * analyses are seeded before the observation families for readability.
 */
final class SoilSeeding {

    private SoilSeeding() {}

    static void seed(DataSource dataSource, NaturalistDatabase database) {
        Seeding.applySchema(dataSource, "schema/soil.sql");
        SoilProfileInfoSeeder.seed(dataSource, database);
        LabAnalysisInfoSeeder.seed(dataSource, database);
        NutrientReadingSeeder.seed(dataSource, database);
        ReportedOptimumSeeder.seed(dataSource, database);
        ReportedRecommendationSeeder.seed(dataSource, database);
        SoilPhysicalCharacteristicsSeeder.seed(dataSource, database);
    }
}
