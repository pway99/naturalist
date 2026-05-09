package com.naturalist.plants;

import com.naturalist.data.EntityQuery;
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
 * plantQuery.plants().findPage(PageRequest.console(0));
 * plantQuery.families().getByName(familyName);
 * plantQuery.genera().getByName(genusName);
 * }</pre>
 */
public interface PlantQuery {

    PlantEntityQuery plants();

    PlantFamilyEntityQuery families();

    PlantGenusEntityQuery genera();

    interface PlantEntityQuery extends EntityQuery<PlantName, Plant, PlantCollection> {
    }

    interface PlantFamilyEntityQuery
            extends EntityQuery<PlantFamilyName, PlantFamily, PlantFamilyCollection> {
    }

    interface PlantGenusEntityQuery
            extends EntityQuery<PlantGenusName, PlantGenus, PlantGenusCollection> {
    }
}
