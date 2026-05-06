package com.naturalist.plants;

import com.naturalist.data.EntityQuery;
import com.naturalist.ddd.EntityNameSet;
import com.naturalist.plants.PlantEntityCollections.PlantCollection;
import com.naturalist.plants.PlantEntityCollections.PlantFamilyCollection;
import com.naturalist.plants.PlantEntityCollections.PlantGenusCollection;

/**
 * Namespace query for the plants top-level sub-context — the single
 * discoverable entry point for reading plant catalog data.
 *
 * <p>Nested queries scope to a single entity each:
 * <ul>
 *   <li>{@link PlantEntityQuery} — {@link Plant} entities.</li>
 *   <li>{@link PlantFamilyEntityQuery} — {@link PlantFamily} entities.</li>
 *   <li>{@link PlantGenusEntityQuery} — {@link PlantGenus} entities.</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * plantQuery.plants().getByName(plantName);
 * plantQuery.plants().allPlantNames();
 * plantQuery.families().getByName(familyName);
 * plantQuery.genera().getByName(genusName);
 * }</pre>
 */
public interface PlantQuery {

    PlantEntityQuery plants();

    PlantFamilyEntityQuery families();

    PlantGenusEntityQuery genera();

    interface PlantEntityQuery extends EntityQuery<PlantName, Plant, PlantCollection> {

        EntityNameSet<PlantName> allPlantNames();
    }

    interface PlantFamilyEntityQuery
            extends EntityQuery<PlantFamilyName, PlantFamily, PlantFamilyCollection> {

        EntityNameSet<PlantFamilyName> allFamilyNames();
    }

    interface PlantGenusEntityQuery
            extends EntityQuery<PlantGenusName, PlantGenus, PlantGenusCollection> {

        EntityNameSet<PlantGenusName> allGenusNames();
    }
}
