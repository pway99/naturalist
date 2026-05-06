package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class PlantGenusTestEntitySource extends TestEntitySource<PlantGenusName, PlantGenus> {

    public PlantGenusTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plant-genera.json");
    }
}
