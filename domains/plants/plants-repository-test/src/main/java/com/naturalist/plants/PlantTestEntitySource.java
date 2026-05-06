package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class PlantTestEntitySource extends TestEntitySource<PlantName, Plant> {

    public PlantTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plants.json");
    }
}
