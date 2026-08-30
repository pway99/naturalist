package com.naturalist.insects;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the app-level {@code catalog_search_token} fan-in view (created by
 * {@code apps/test-db-seeder}'s {@code CatalogSeeding}, applied last by {@code TestDbSeeder}) rather
 * than any insects-owned schema. It lives here — not in {@code apps/test-db-seeder} — because that
 * module has no failsafe/IT harness (see task-3-brief Step 1); {@code insects-repository-rdbms}
 * already carries {@code RdbmsTestExtension} and is the sole participant unioned into the view so far.
 */
class CatalogSearchViewIT {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    private int count(String sql) throws Exception {
        try (Statement st = rdbms.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next(); return rs.getInt(1);
        }
    }

    @Test
    void fanInViewReturnsInsectRows() throws Exception {
        assertTrue(count("SELECT count(*) FROM catalog_search_token WHERE domain = 'insects'") > 0);
    }
}
