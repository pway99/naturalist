package com.naturalist.library;

import com.naturalist.persistence.test.DboSchemaValidator;
import com.naturalist.persistence.test.RdbmsDataSource;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Anti-drift guard for the library ACL. Builds the schema from {@code schema/library.sql} in a
 * throwaway Postgres schema and asserts it matches every {@link com.naturalist.persistence.DboSchema}
 * declared in the module (discovered by classpath scan). Pins the DDL file, the DBO metadata, and the
 * live database together, so a mistyped column, a missing constraint, or a stale {@code schema.sql}
 * fails the build instead of silently mis-mapping at runtime. Runs on its own connection (no seed data
 * required).
 */
class LibrarySchemaDriftIT {

    private static final String SCRATCH = "library_drift_check";

    @Test
    void ddlMatchesEveryDboSchema() throws Exception {
        try (Connection c = RdbmsDataSource.shared().getConnection()) {
            c.setAutoCommit(true);
            exec(c, "DROP SCHEMA IF EXISTS " + SCRATCH + " CASCADE");
            exec(c, "CREATE SCHEMA " + SCRATCH);
            try {
                exec(c, "SET search_path TO " + SCRATCH);
                applyDdl(c, "schema/library.sql");
                List<String> problems =
                        DboSchemaValidator.validate(c, SCRATCH, DboSchemaValidator.discover("com.naturalist.library"));
                assertThat(problems).as("schema drift").isEmpty();
            } finally {
                exec(c, "DROP SCHEMA IF EXISTS " + SCRATCH + " CASCADE");
                exec(c, "SET search_path TO public");
            }
        }
    }

    private static void applyDdl(Connection c, String resource) throws Exception {
        String sql;
        try (InputStream in = LibrarySchemaDriftIT.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("missing resource " + resource);
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String stmt : sql.split(";")) {
            if (!stmt.isBlank()) exec(c, stmt);
        }
    }

    private static void exec(Connection c, String sql) throws Exception {
        try (Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }
}
