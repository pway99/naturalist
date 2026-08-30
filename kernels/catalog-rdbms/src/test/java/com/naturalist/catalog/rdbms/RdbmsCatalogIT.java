package com.naturalist.catalog.rdbms;

import com.naturalist.catalog.*;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectRankName;
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

    // The shared standing Postgres view (catalog_search_token) now fans in plants
    // alongside insects (see RdbmsCatalogPlantsIT), and RdbmsCatalogAssembly#from
    // validates its registry against *every* domain/entity_type the live view emits
    // — not just the ones a given test cares about. A registry naming only insects
    // therefore fails startup validation the moment plants participates too, so this
    // helper (and the DomainId list below) covers both.
    private record TestPlants() implements DomainId {
        @Override
        public String value() {
            return "plants";
        }
    }

    private Catalog catalog() {
        Map<String, Function<String, EntityName>> reconstructors = Map.of(
                "insect-order",   InsectOrderName::of,
                "insect-family",  InsectFamilyName::of,
                "insect-genus",   InsectGenusName::of,
                "insect-species", InsectSpeciesName::of,
                "plant-order",    PlantOrderName::of,
                "plant-family",   PlantFamilyName::of,
                "plant-genus",    PlantGenusName::of,
                "plant-species",  PlantSpeciesName::of);
        return RdbmsCatalogAssembly.from(rdbms.mapper(CatalogSearchMapper.class),
                List.of(new TestInsects(), new TestPlants()), reconstructors, List.of(), Resilience.noOp());
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

    @Test
    void assemblyThrowsWhenRegistryIsMissingAnEntityTypeTheViewEmits() {
        // Omits "insect-order", which the live seeded view does emit (see
        // CatalogSearchMapperIT#distinctDomainTypesCoversInsects) — the
        // startup validation pass must reject this registry before any
        // query can silently drop rows. Plants entity types are included (and
        // TestPlants registered as a domain) so this exercises the entity-type
        // gap specifically, not the domain gap covered by the other tests.
        Map<String, Function<String, EntityName>> incomplete = Map.of(
                "insect-family",  InsectFamilyName::of,
                "insect-genus",   InsectGenusName::of,
                "insect-species", InsectSpeciesName::of,
                "plant-order",    PlantOrderName::of,
                "plant-family",   PlantFamilyName::of,
                "plant-genus",    PlantGenusName::of,
                "plant-species",  PlantSpeciesName::of);

        assertThrows(IllegalStateException.class, () -> RdbmsCatalogAssembly.from(
                rdbms.mapper(CatalogSearchMapper.class),
                List.of(new TestInsects(), new TestPlants()), incomplete, List.of(), Resilience.noOp()));
    }

    @Test
    void assemblyThrowsOnDuplicateDomainSlug() {
        record RogueInsects() implements DomainId {
            @Override
            public String value() {
                return "insects";
            }
        }
        Map<String, Function<String, EntityName>> reconstructors = Map.of(
                "insect-order",   InsectOrderName::of,
                "insect-family",  InsectFamilyName::of,
                "insect-genus",   InsectGenusName::of,
                "insect-species", InsectSpeciesName::of);

        assertThrows(IllegalArgumentException.class, () -> RdbmsCatalogAssembly.from(
                rdbms.mapper(CatalogSearchMapper.class),
                List.of(new TestInsects(), new RogueInsects()), reconstructors, List.of(), Resilience.noOp()));
    }
}
