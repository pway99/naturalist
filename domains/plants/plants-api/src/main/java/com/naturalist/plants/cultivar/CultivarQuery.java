package com.naturalist.plants.cultivar;

import com.naturalist.data.EntityQuery;
import com.naturalist.plants.PlantSpeciesName;

/**
 * Read surface for the cultivar sub-context. N=1 collapse (ADR-020): the sub-context
 * holds a single entity, so this query <em>is</em> the entity query — no wrapping
 * namespace, no accessor. Mirrors the top-level convention insects uses.
 */
public interface CultivarQuery extends EntityQuery<CultivarName, Cultivar, CultivarCollection> {

    CultivarCollection forPlantName(PlantSpeciesName plantSpeciesName);
}
