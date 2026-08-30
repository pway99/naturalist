package com.naturalist.seeder;

import javax.sql.DataSource;

/**
 * Applies the app-level {@code catalog_search_token} fan-in view over the per-domain
 * {@code <domain>_catalog_token} views. MUST run after every participating domain has been seeded —
 * it is a plain {@code CREATE VIEW}, not data, so re-running it is cheap and always safe.
 */
final class CatalogSeeding {

    private CatalogSeeding() {}

    static void apply(DataSource dataSource) {
        Seeding.applySchema(dataSource, "schema/catalog.sql");
    }
}
