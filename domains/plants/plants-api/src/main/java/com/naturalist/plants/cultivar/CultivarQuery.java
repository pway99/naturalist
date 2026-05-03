package com.naturalist.plants.cultivar;

import com.naturalist.data.EntityQuery;
import com.naturalist.ddd.EntityNameSet;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.cultivar.CultivarEntityCollections.CultivarCollection;

/**
 * Namespace query for the cultivar sub-context — the single discoverable
 * entry point for reading cultivar data.
 */
public interface CultivarQuery {

    CultivarEntityQuery cultivars();

    interface CultivarEntityQuery extends EntityQuery<CultivarName, Cultivar, CultivarCollection> {

        EntityNameSet<CultivarName> allCultivarNames();

        /**
         * All cultivars recorded for a given plant — the natural plant → cultivars rollup.
         */
        CultivarCollection forPlantName(PlantName plantName);
    }
}
