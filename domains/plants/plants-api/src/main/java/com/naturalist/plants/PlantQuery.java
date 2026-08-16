package com.naturalist.plants;

import com.naturalist.data.EntityQuery;

import java.util.Optional;
import com.naturalist.plants.PlantEntityCollections.PlantSpeciesCollection;
import com.naturalist.plants.PlantEntityCollections.PlantFamilyCollection;
import com.naturalist.plants.PlantEntityCollections.PlantGenusCollection;

/**
 * Namespace query for the plants top-level sub-context — the single
 * discoverable entry point for reading plant catalog data.
 *
 * <p>Nested queries scope to a single entity each:
 * <ul>
 *   <li>{@link PlantEntityQuery} — {@link PlantSpecies} entities.</li>
 *   <li>{@link PlantFamilyEntityQuery} — {@link PlantFamily} entities.</li>
 *   <li>{@link PlantGenusEntityQuery} — {@link PlantGenus} entities.</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * plantQuery.plants().getByName(plantName);
 * plantQuery.plants().findPage(PageRequest.console(0));
 * plantQuery.families().getByName(familyName);
 * plantQuery.genera().getByName(genusName);
 * plantQuery.genera().forFamilyName(familyName);   // genera under a family
 * }</pre>
 */
public interface PlantQuery {

    PlantEntityQuery plants();

    PlantFamilyEntityQuery families();

    PlantGenusEntityQuery genera();

    interface PlantEntityQuery extends EntityQuery<PlantSpeciesName, PlantSpecies, PlantSpeciesCollection> {
    }

    interface PlantFamilyEntityQuery
            extends EntityQuery<PlantFamilyName, PlantFamily, PlantFamilyCollection> {
    }

    PlantEcologicalRoleEntityQuery ecologicalRoles();

    interface PlantGenusEntityQuery
            extends EntityQuery<PlantGenusName, PlantGenus, PlantGenusCollection> {

        /**
         * Genera under a family, joined on the genus's typed
         * {@link PlantGenus#familyName()} upward FK — the natural
         * family &rarr; genera rollup the family detail page renders.
         */
        PlantGenusCollection forFamilyName(PlantFamilyName familyName);
    }

    interface PlantEcologicalRoleEntityQuery
            extends EntityQuery<PlantEcologicalRoleId, PlantEcologicalRole,
                    PlantEntityCollections.PlantEcologicalRoleCollection> {

        /**
         * The ecological role recorded for a taxon, at whatever rank it was recorded.
         * Empty when the taxon's ecology has not been characterised — which is a real
         * state, not a missing record.
         */
        Optional<PlantEcologicalRole> forPlantName(PlantRankName plantName);
    }
}
