package com.naturalist.catalog.rdbms;

import com.naturalist.catalog.*;
import com.naturalist.persistence.test.RdbmsTestExtension;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.resilience.Resilience;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RdbmsCatalogPlantsIT {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    // The shared standing Postgres view (catalog_search_token) fans in insects, plants, and
    // chemistry, and RdbmsCatalogAssembly#from validates its registry against *every*
    // domain/entity_type the live view emits — not just the ones this test cares about (see
    // RdbmsCatalogIT for the mirror image of this same coupling). CatalogTestRegistry
    // centralizes the full registry so this IT does not need its own inline copy.
    private Catalog catalog() {
        return RdbmsCatalogAssembly.from(rdbms.mapper(CatalogSearchMapper.class),
                CatalogTestRegistry.domains(), CatalogTestRegistry.reconstructors(), List.of(), Resilience.noOp());
    }

    @Test
    void searchReturnsTypedPlantRefs() {
        SearchResults results = catalog().search("aristolochia");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(h -> "plants".equals(h.target().domain().value())));
    }

    @Test
    void findBySlugResolvesToTypedSpeciesRef() {
        var ref = catalog().findBySlug("aristolochia-californica");
        assertTrue(ref.isPresent());
        assertTrue(ref.get().name() instanceof PlantSpeciesName);
    }
}
