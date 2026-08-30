package com.naturalist.catalog.rdbms;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.SearchResults;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.persistence.test.RdbmsTestExtension;
import com.naturalist.resilience.Resilience;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RdbmsCatalogChemistryIT {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    private Catalog catalog() {
        return RdbmsCatalogAssembly.from(rdbms.mapper(CatalogSearchMapper.class),
                CatalogTestRegistry.domains(), CatalogTestRegistry.reconstructors(),
                List.of(), Resilience.noOp());
    }

    @Test
    void searchReturnsTypedChemistryRefs() {
        SearchResults results = catalog().search("azadirachtin");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(h -> "chemistry".equals(h.target().domain().value())));
    }

    @Test
    void findBySlugResolvesToTypedCompoundRef() {
        var ref = catalog().findBySlug("azadirachtin");
        assertTrue(ref.isPresent());
        assertTrue(ref.get().name() instanceof CompoundName);
    }
}
