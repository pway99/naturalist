package com.naturalist.plants.management;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.plants.PlantTestEntitySource;

import java.util.List;

public class PlantProgramTestEntitySource extends TestEntitySource<PlantProgramName, PlantProgram> {

    public PlantProgramTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/management/plant-programs.json");
    }

    @Override
    protected List<ForeignKeyConstraint<PlantProgram, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "plantName",
                PlantProgram::plantName,
                PlantTestEntitySource.class));
    }
}
