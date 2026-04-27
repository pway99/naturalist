package com.naturalist.plants.management;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

public class PlantProgramEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantProgramName, PlantProgram, PlantProgramTestEntitySource>
        implements PlantProgramRepository.PlantProgramEntityRepository {

    protected PlantProgramEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
