package com.naturalist.plants;

import com.naturalist.data.NamedTestEntitySource;

public class PlantTestEntitySource extends NamedTestEntitySource<PlantName, Plant> {

    public PlantTestEntitySource() {
        loadFile("plants/plants.json");
    }
}
