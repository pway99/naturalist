package com.naturalist.plants;

import com.naturalist.data.TestEntitySource;

public class PlantTestEntitySource extends TestEntitySource<PlantName, Plant> {

    public PlantTestEntitySource() {
        loadFile("plants/plants.json");
    }
}
