package com.naturalist.seeder;

import com.naturalist.persistence.test.SchemaApplier;

import javax.sql.DataSource;
import java.sql.Connection;

/** Shared helper for the per-domain seeders. */
final class Seeding {

    private Seeding() {}

    /** Drop + recreate a domain's tables from its {@code schema/*.sql} DDL resource. */
    static void applySchema(DataSource dataSource, String resource) {
        try (Connection connection = dataSource.getConnection()) {
            // SchemaApplier commits explicitly, so autoCommit must be off or Postgres rejects it.
            connection.setAutoCommit(false);
            SchemaApplier.applyResource(connection, resource);
        } catch (Exception e) {
            throw new IllegalStateException("Failed applying schema " + resource, e);
        }
    }
}
