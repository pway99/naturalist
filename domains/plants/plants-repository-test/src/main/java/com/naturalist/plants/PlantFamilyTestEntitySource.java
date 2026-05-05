package com.naturalist.plants;

import com.naturalist.data.TestEntitySource;

public class PlantFamilyTestEntitySource extends TestEntitySource<PlantFamilyName, PlantFamily> {

    public PlantFamilyTestEntitySource() {
        loadFile("plants/plant-families.json");
    }
}
