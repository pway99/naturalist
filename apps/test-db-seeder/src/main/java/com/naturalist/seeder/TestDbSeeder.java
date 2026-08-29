package com.naturalist.seeder;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.test.RdbmsDataSource;

import javax.sql.DataSource;
import java.util.Set;

/**
 * Materializes the JSON seed into the standing Postgres. Run after cloning and whenever seed JSON
 * or DDL changes. Each domain is a self-contained seeder class (applies its own schema, then its
 * data) so one can be run or fixed in isolation. Idempotent: every domain drops + recreates its
 * tables before reseeding.
 *
 * <p>With no arguments, seeds every domain. Pass domain names ({@code naturalists}, {@code chemistry})
 * to seed only those — e.g. {@code -Dexec.args="chemistry"} to reseed chemistry alone.
 */
public final class TestDbSeeder {

    public static void main(String[] args) {
        Set<String> only = Set.of(args);

        // RdbmsDataSource.shared() is a process-wide singleton, intentionally never closed here:
        // the process exits right after main() returns and Hikari's pool threads are daemon
        // threads, so nothing hangs. Do NOT wrap it in try-with-resources — that would tear the
        // shared pool down for anything else in the JVM.
        DataSource dataSource = RdbmsDataSource.shared();
        NaturalistDatabase database = NaturalistDatabase.create();

        if (only.isEmpty() || only.contains("naturalists")) NaturalistSeeding.seed(dataSource, database);
        if (only.isEmpty() || only.contains("chemistry")) ChemistrySeeding.seed(dataSource, database);

        System.out.println("Seed complete" + (only.isEmpty() ? "" : " (" + String.join(", ", only) + ")") + ".");
    }
}
