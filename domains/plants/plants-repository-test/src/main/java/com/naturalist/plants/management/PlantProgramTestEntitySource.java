package com.naturalist.plants.management;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

/**
 * Loads the plant-management program catalog from JSON at test time.
 * <p>
 * No {@code ForeignKeyConstraint} on {@code plantName}: it is a
 * {@link com.naturalist.plants.PlantRankName} — a program can target a genus as
 * readily as a species — and the framework's FK check resolves a single source class,
 * while this spans the whole rank chain. {@code PlantProgramCatalogDataTest} covers the
 * referential integrity instead.
 */
public class PlantProgramTestEntitySource extends TestEntitySource<PlantProgramName, PlantProgram> {

    public PlantProgramTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/management/plant-programs.json");
    }
}
