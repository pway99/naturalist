package com.naturalist.catalog.rdbms;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogSearchMapperIT {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    private CatalogSearchMapper mapper() { return rdbms.mapper(CatalogSearchMapper.class); }

    @Test
    void substringSearchReturnsClassifiedRows() {
        List<CatalogTokenRow> rows = mapper().search("apis"); // matches seeded species apis-mellifera
        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().allMatch(r -> r.slug != null && r.domain != null && r.entityType != null));
        assertTrue(rows.stream().allMatch(r ->
                List.of("EXACT_SLUG","EXACT_TOKEN","PREFIX","FUZZY").contains(r.kind)));
    }

    @Test
    void distinctDomainTypesCoversInsects() {
        assertTrue(mapper().distinctDomainTypes().stream().anyMatch(r -> "insects".equals(r.domain)));
    }
}
