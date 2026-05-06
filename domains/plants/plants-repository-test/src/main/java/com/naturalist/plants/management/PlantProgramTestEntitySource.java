package com.naturalist.plants.management;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class PlantProgramTestEntitySource extends TestEntitySource<PlantProgramName, PlantProgram> {

    public PlantProgramTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/management/plant-programs.json");
    }
}
