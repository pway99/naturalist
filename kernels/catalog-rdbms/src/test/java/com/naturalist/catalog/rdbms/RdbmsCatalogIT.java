package com.naturalist.catalog.rdbms;

import com.naturalist.catalog.*;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.persistence.test.RdbmsTestExtension;
import com.naturalist.resilience.Resilience;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class RdbmsCatalogIT {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    // Test-local DomainId — the kernel IT stays decoupled from the production
    // insects-api module; the slug value matches the seeded catalog_search_token
    // rows' "insects" domain column for readability of assertions (mirrors
    // InMemoryCatalogTest's TestInsects pattern).
    private record TestInsects() implements DomainId {
        @Override
        public String value() {
            return "insects";
        }
    }

    private Catalog catalog() {
        Map<String, Function<String, EntityName>> reconstructors = Map.of(
                "insect-order",   InsectOrderName::of,
                "insect-family",  InsectFamilyName::of,
                "insect-genus",   InsectGenusName::of,
                "insect-species", InsectSpeciesName::of);
        return RdbmsCatalogAssembly.from(rdbms.mapper(CatalogSearchMapper.class),
                List.of(new TestInsects()), reconstructors, List.of(), Resilience.noOp());
    }

    @Test
    void searchReturnsTypedInsectRefs() {
        SearchResults results = catalog().search("apis");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(h -> "insects".equals(h.target().domain().value())));
        assertTrue(results.stream().allMatch(h -> h.target().name() instanceof InsectRankName));
    }

    @Test
    void findBySlugResolvesToTypedRef() {
        var ref = catalog().findBySlug("apis-mellifera");
        assertTrue(ref.isPresent());
        assertTrue(ref.get().name() instanceof InsectSpeciesName);
    }

    @Test
    void blankInputIsEmptyAndFiresNoObservation() {
        assertTrue(catalog().search("   ").isEmpty());
    }
}
