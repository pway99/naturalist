package com.naturalist.plants;

import com.naturalist.data.EntityQuery;
import com.naturalist.ddd.EntityNameSet;
import com.naturalist.plants.PlantEntityCollections.PlantCollection;

/**
 * Namespace query for the plants top-level sub-context — the single
 * discoverable entry point for reading plant catalog data.
 *
 * <p>Nested queries scope to a single entity each:
 * <ul>
 *   <li>{@link PlantEntityQuery} — {@link Plant} entities.</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * plantQuery.plants().getByName(plantName);
 * plantQuery.plants().allPlantNames();
 * }</pre>
 */
public interface PlantQuery {

    PlantEntityQuery plants();

    interface PlantEntityQuery extends EntityQuery<PlantName, Plant, PlantCollection> {

        EntityNameSet<PlantName> allPlantNames();
    }
}
