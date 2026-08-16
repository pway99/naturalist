package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

/**
 * The 13 plant orders represented at Oak Vista — the top of the rank chain, so this source
 * declares no foreign key of its own. Every other rank source points, directly or
 * transitively, at one of these records.
 */
public class PlantOrderTestEntitySource extends TestEntitySource<PlantOrderName, PlantOrder> {

    public PlantOrderTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plant-orders.json");
    }
}
