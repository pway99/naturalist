package com.naturalist.seeder;

import com.naturalist.chemistry.compound.CompoundSeeder;
import com.naturalist.chemistry.element.ElementSeeder;
import com.naturalist.chemistry.product.ProductSeeder;
import com.naturalist.data.NaturalistDatabase;

import javax.sql.DataSource;

/**
 * Seeds the chemistry domain, in FK order: elements, then compounds + depictions, then products.
 * Each entity's seeder lives in that entity's own package (split-package with the rdbms module),
 * so this orchestrator only sequences them — no chemistry internals are exposed publicly.
 */
final class ChemistrySeeding {

    private ChemistrySeeding() {}

    static void seed(DataSource dataSource, NaturalistDatabase database) {
        Seeding.applySchema(dataSource, "schema/chemistry.sql");
        loadData(dataSource, database);
    }

    /** Loads the data only, into a schema that already exists. */
    static void loadData(DataSource dataSource, NaturalistDatabase database) {
        ElementSeeder.seed(dataSource, database);
        CompoundSeeder.seed(dataSource, database);
        ProductSeeder.seed(dataSource, database);
    }
}
