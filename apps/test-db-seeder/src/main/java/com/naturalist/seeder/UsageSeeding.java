package com.naturalist.seeder;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.usage.UsageCounterSeeder;

import javax.sql.DataSource;

/**
 * Seeds the usage domain. Applies the schema (creating usage_counter/usage_event/usage_alert) and seeds the
 * counter rules from JSON; usage events and alerts carry no persistent seed (they are runtime append-only).
 */
final class UsageSeeding {

    private UsageSeeding() {}

    static void seed(DataSource dataSource, NaturalistDatabase database) {
        Seeding.applySchema(dataSource, "schema/usage.sql");
        loadData(dataSource, database);
    }

    /** Loads the data only, into a schema that already exists. */
    static void loadData(DataSource dataSource, NaturalistDatabase database) {
        UsageCounterSeeder.seed(dataSource, database);
    }
}
