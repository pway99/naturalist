package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class PlantFamilyTestEntitySource extends TestEntitySource<PlantFamilyName, PlantFamily> {

    public PlantFamilyTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plant-families.json");
    }
}
