package com.naturalist.seeder;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.garden.PlantingSeeder;

import javax.sql.DataSource;

/** Seeds the garden domain — a single flat {@code planting} table with no foreign keys. */
final class GardenSeeding {

    private GardenSeeding() {}

    static void seed(DataSource dataSource, NaturalistDatabase database) {
        Seeding.applySchema(dataSource, "schema/garden.sql");
        PlantingSeeder.seed(dataSource, database);
    }
}
