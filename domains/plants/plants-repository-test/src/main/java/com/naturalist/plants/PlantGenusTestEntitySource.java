package com.naturalist.plants;

import com.naturalist.data.TestEntitySource;

public class PlantGenusTestEntitySource extends TestEntitySource<PlantGenusName, PlantGenus> {

    public PlantGenusTestEntitySource() {
        loadFile("plants/plant-genera.json");
    }
}
