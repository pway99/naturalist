package com.naturalist.plants;

import com.naturalist.data.EntityQuery;

import java.util.Optional;
import com.naturalist.plants.PlantEntityCollections.SpeciesCollection;
import com.naturalist.plants.PlantEntityCollections.FamilyCollection;
import com.naturalist.plants.PlantEntityCollections.GenusCollection;

/**
 * Namespace query for the plants top-level sub-context — the single
 * discoverable entry point for reading plant catalog data.
 *
 * <p>Nested queries scope to a single entity each:
 * <ul>
 *   <li>{@link SpeciesQuery} — {@link PlantSpecies} entities.</li>
 *   <li>{@link FamilyQuery} — {@link PlantFamily} entities.</li>
 *   <li>{@link GenusQuery} — {@link PlantGenus} entities.</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * plantQuery.species().getByName(plantName);
 * plantQuery.species().findPage(PageRequest.console(0));
 * plantQuery.families().getByName(familyName);
 * plantQuery.genera().getByName(genusName);
 * plantQuery.genera().forFamilyName(familyName);   // genera under a family
 * }</pre>
 */
public interface PlantQuery {

    SpeciesQuery species();

    OrderQuery orders();

    FamilyQuery families();

    GenusQuery genera();

    interface SpeciesQuery extends EntityQuery<PlantSpeciesName, PlantSpecies, SpeciesCollection> {

        /**
         * Species under a genus, joined on the species' typed
         * {@link PlantSpecies#genusName()} upward FK — the genus &rarr; species rollup
         * the genus detail page renders.
         */
        SpeciesCollection forGenusName(PlantGenusName genusName);

        /**
         * Species under a family, composed through the genus query: every genus in the
         * family, then every species in each genus. Mirrors {@code InsectQuery.SpeciesQuery}.
         */
        SpeciesCollection forFamilyName(PlantFamilyName familyName);
    }

    interface OrderQuery
            extends EntityQuery<PlantOrderName, PlantOrder,
                    PlantEntityCollections.OrderCollection> {
    }

    interface FamilyQuery
            extends EntityQuery<PlantFamilyName, PlantFamily, FamilyCollection> {

        /** Families under an order, joined on the family's typed upward FK. */
        FamilyCollection forOrderName(PlantOrderName orderName);
    }

    EcologicalRoleQuery ecologicalRoles();

    interface GenusQuery
            extends EntityQuery<PlantGenusName, PlantGenus, GenusCollection> {

        /**
         * Genera under a family, joined on the genus's typed
         * {@link PlantGenus#familyName()} upward FK — the natural
         * family &rarr; genera rollup the family detail page renders.
         */
        GenusCollection forFamilyName(PlantFamilyName familyName);
    }

    interface EcologicalRoleQuery
            extends EntityQuery<PlantEcologicalRoleId, PlantEcologicalRole,
                    PlantEntityCollections.EcologicalRoleCollection> {

        /**
         * The ecological role recorded for a taxon, at whatever rank it was recorded.
         * Empty when the taxon's ecology has not been characterised — which is a real
         * state, not a missing record.
         */
        Optional<PlantEcologicalRole> forPlantName(PlantRankName plantName);
    }
}
