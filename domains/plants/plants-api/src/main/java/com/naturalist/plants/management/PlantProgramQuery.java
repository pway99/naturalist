package com.naturalist.plants.management;

import com.naturalist.data.EntityQuery;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.management.PlantProgramEntityCollections.PlantProgramCollection;

/**
 * Namespace query for the management sub-context — the single discoverable
 * entry point for reading plant-program data.
 */
public interface PlantProgramQuery {

    PlantProgramEntityQuery programs();

    interface PlantProgramEntityQuery extends EntityQuery<PlantProgramName, PlantProgram, PlantProgramCollection> {

        /**
         * All programs recorded for a given plant — the natural plant → programs rollup.
         */
        PlantProgramCollection forPlantName(PlantName plantName);
    }
}
