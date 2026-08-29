package com.naturalist.seeder;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantEcologicalRoleSeeder;
import com.naturalist.plants.PlantFamilySeeder;
import com.naturalist.plants.PlantFeatureAssignmentSeeder;
import com.naturalist.plants.PlantFeatureSeeder;
import com.naturalist.plants.PlantGenusSeeder;
import com.naturalist.plants.PlantImageSeeder;
import com.naturalist.plants.PlantObservationSeeder;
import com.naturalist.plants.PlantOrderSeeder;
import com.naturalist.plants.PlantSpeciesSeeder;
import com.naturalist.plants.cultivar.CultivarSeeder;
import com.naturalist.plants.heritage.SeedLineageSeeder;
import com.naturalist.plants.management.PlantProgramSeeder;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentSeeder;

import javax.sql.DataSource;

/**
 * Seeds the plants domain in foreign-key order: the rank chain (order→family→genus→species) first, then
 * features before their assignments, observations before the images that reference them, cultivars before
 * their seed lineages; the cross-rank / cross-domain refs (ecological role, program, phytochemistry) carry
 * no FK and can seed any time after their targets. Each seeder commits its own session, so a parent is
 * durable before its child seeder runs.
 */
final class PlantsSeeding {

    private PlantsSeeding() {}

    static void seed(DataSource dataSource, NaturalistDatabase database) {
        Seeding.applySchema(dataSource, "schema/plants.sql");
        PlantOrderSeeder.seed(dataSource, database);
        PlantFamilySeeder.seed(dataSource, database);
        PlantGenusSeeder.seed(dataSource, database);
        PlantSpeciesSeeder.seed(dataSource, database);
        PlantFeatureSeeder.seed(dataSource, database);
        PlantFeatureAssignmentSeeder.seed(dataSource, database);
        PlantEcologicalRoleSeeder.seed(dataSource, database);
        PlantObservationSeeder.seed(dataSource, database);
        PlantImageSeeder.seed(dataSource, database);
        CultivarSeeder.seed(dataSource, database);
        SeedLineageSeeder.seed(dataSource, database);
        PlantProgramSeeder.seed(dataSource, database);
        PhytochemicalConstituentSeeder.seed(dataSource, database);
    }
}
