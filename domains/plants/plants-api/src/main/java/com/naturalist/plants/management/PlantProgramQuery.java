package com.naturalist.plants.management;

import com.naturalist.data.EntityQuery;
import com.naturalist.plants.PlantRankName;

/**
 * Read surface for the management sub-context. N=1 collapse (ADR-020): the sub-context
 * holds a single entity, so this query <em>is</em> the entity query — no wrapping
 * namespace, no accessor. Mirrors the top-level convention insects uses.
 */
public interface PlantProgramQuery extends EntityQuery<PlantProgramName, PlantProgram, PlantProgramCollection> {

    PlantProgramCollection forPlantName(PlantRankName plantRankName);
}
