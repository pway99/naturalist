package com.naturalist.insects;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class InsectCatalogTokenViewIT {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    private int count(String sql) throws Exception {
        try (Statement st = rdbms.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next(); return rs.getInt(1);
        }
    }

    @Test
    void viewExposesSlugRowsForEverySpecies() throws Exception {
        assertTrue(count("SELECT count(*) FROM insect_catalog_token "
                + "WHERE is_slug AND entity_type = 'insect-species'") > 0);
    }

    @Test
    void viewExposesFourEntityTypes() throws Exception {
        assertEquals(4, count("SELECT count(DISTINCT entity_type) FROM insect_catalog_token"));
    }
}
