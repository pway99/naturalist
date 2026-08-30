package com.naturalist.catalog.rdbms;

import com.naturalist.catalog.DomainId;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.chemistry.element.ElementName;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.plants.PlantFamilyName;
import com.naturalist.plants.PlantGenusName;
import com.naturalist.plants.PlantOrderName;
import com.naturalist.plants.PlantSpeciesName;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Shared full registry for the {@code kernels/catalog-rdbms} IT suite. The standing Postgres
 * {@code catalog_search_token} view fans in every participating domain, and
 * {@link com.naturalist.catalog.rdbms.RdbmsCatalogAssembly#from} validates its registry against
 * *every* domain/entity_type the live view emits — not just the ones a given test cares about
 * (see {@code RdbmsCatalogAssembly#validateRegistry}). Each per-domain IT (insects, plants,
 * chemistry) therefore needs the full registry, not just its own slice; centralizing it here
 * means adding a new participating domain touches one file instead of forcing every existing IT
 * to widen its own inline copy again (as plants did to {@code RdbmsCatalogIT}, and chemistry
 * would otherwise do to both insects and plants).
 */
final class CatalogTestRegistry {

    private CatalogTestRegistry() {}

    // Test-local DomainId records — kept decoupled from the production *-api modules. Slug
    // values match the seeded catalog_search_token rows' domain column.
    private record TestInsects() implements DomainId {
        @Override
        public String value() {
            return "insects";
        }
    }

    private record TestPlants() implements DomainId {
        @Override
        public String value() {
            return "plants";
        }
    }

    private record TestChemistry() implements DomainId {
        @Override
        public String value() {
            return "chemistry";
        }
    }

    static List<DomainId> domains() {
        return List.of(new TestInsects(), new TestPlants(), new TestChemistry());
    }

    static Map<String, Function<String, EntityName>> reconstructors() {
        return Map.of(
                "insect-order",   InsectOrderName::of,
                "insect-family",  InsectFamilyName::of,
                "insect-genus",   InsectGenusName::of,
                "insect-species", InsectSpeciesName::of,
                "plant-order",    PlantOrderName::of,
                "plant-family",   PlantFamilyName::of,
                "plant-genus",    PlantGenusName::of,
                "plant-species",  PlantSpeciesName::of,
                "element",        ElementName::of,
                "compound",       CompoundName::of);
    }
}
