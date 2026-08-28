package com.naturalist.seeder;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.naturalist.Naturalist;
import com.naturalist.naturalist.NaturalistCredential;
import com.naturalist.naturalist.NaturalistCredentialTestEntitySource;
import com.naturalist.naturalist.NaturalistRdbmsSeed;
import com.naturalist.naturalist.NaturalistTestEntitySource;
import com.naturalist.persistence.test.RdbmsDataSource;
import com.naturalist.persistence.test.SchemaApplier;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

/** Materializes the JSON seed into the standing Postgres. Run after cloning and whenever
 *  seed JSON or DDL changes. Idempotent: drops + recreates the schema, then reseeds. */
public final class TestDbSeeder {

    public static void main(String[] args) throws Exception {
        // RdbmsDataSource.shared() is a process-wide singleton, intentionally never closed
        // here: the process exits right after main() returns, and Hikari's pool threads are
        // daemon threads, so there is nothing to hang on. Do not "fix" this into a
        // try-with-resources — that would tear down the shared pool for any other code
        // running in the same JVM.
        DataSource dataSource = RdbmsDataSource.shared();
        try (Connection connection = dataSource.getConnection()) {
            // SchemaApplier commits explicitly after applying the script; autoCommit must be
            // off, or Postgres rejects the commit() call with "Cannot commit when autoCommit
            // is enabled."
            connection.setAutoCommit(false);
            SchemaApplier.applyResource(connection, "schema/naturalists.sql");
        }

        NaturalistDatabase database = NaturalistDatabase.create();
        List<Naturalist> naturalists =
                database.getNamed(NaturalistTestEntitySource.class).entityStream().toList();
        List<NaturalistCredential> credentials =
                database.getNamed(NaturalistCredentialTestEntitySource.class).entityStream().toList();

        NaturalistRdbmsSeed.seed(dataSource, naturalists, credentials);
        System.out.println("Seed complete.");
    }
}
