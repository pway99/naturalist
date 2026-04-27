package com.naturalist.plants.management;

import com.naturalist.data.TestEntitySource;

public class PlantProgramTestEntitySource extends TestEntitySource<PlantProgramName, PlantProgram> {

    public PlantProgramTestEntitySource() {
        loadFile("plants/management/plant-programs.json");
    }
}
