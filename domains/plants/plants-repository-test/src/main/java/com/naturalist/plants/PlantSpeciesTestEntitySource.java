package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class PlantSpeciesTestEntitySource extends TestEntitySource<PlantSpeciesName, PlantSpecies> {

    public PlantSpeciesTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plant-species.json");
    }
}
