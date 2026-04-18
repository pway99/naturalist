package com.naturalist.plants;

import com.naturalist.data.TestEntitySource;
public class PlantTestEntitySource extends TestEntitySource<PlantId, PlantName, Plant> {

    public PlantTestEntitySource() {
        super(PlantId::of);
        loadFile("plants/plants.json");
    }
}
