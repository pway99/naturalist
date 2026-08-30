package com.naturalist.catalog.rdbms;

import com.naturalist.catalog.*;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.persistence.test.RdbmsTestExtension;
import com.naturalist.plants.PlantFamilyName;
import com.naturalist.plants.PlantGenusName;
import com.naturalist.plants.PlantOrderName;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.resilience.Resilience;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class RdbmsCatalogPlantsIT {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    // Test-local DomainId — kept decoupled from the production plants-api module
    // (mirrors RdbmsCatalogIT's TestInsects pattern); the slug value matches the
    // seeded catalog_search_token rows' "plants" domain column.
    private record TestPlants() implements DomainId {
        @Override
        public String value() {
            return "plants";
        }
    }

    // The shared standing Postgres view (catalog_search_token) fans in insects
    // alongside plants, and RdbmsCatalogAssembly#from validates its registry against
    // *every* domain/entity_type the live view emits — not just the ones this test
    // cares about (see RdbmsCatalogIT for the mirror image of this same coupling).
    // A plants-only registry therefore fails startup validation; this test-local
    // DomainId plus the insect reconstructors below cover the rest of the live view.
    private record TestInsects() implements DomainId {
        @Override
        public String value() {
            return "insects";
        }
    }

    private Catalog catalog() {
        Map<String, Function<String, EntityName>> reconstructors = Map.of(
                "plant-order",    PlantOrderName::of,
                "plant-family",   PlantFamilyName::of,
                "plant-genus",    PlantGenusName::of,
                "plant-species",  PlantSpeciesName::of,
                "insect-order",   InsectOrderName::of,
                "insect-family",  InsectFamilyName::of,
                "insect-genus",   InsectGenusName::of,
                "insect-species", InsectSpeciesName::of);
        return RdbmsCatalogAssembly.from(rdbms.mapper(CatalogSearchMapper.class),
                List.of(new TestPlants(), new TestInsects()), reconstructors, List.of(), Resilience.noOp());
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
