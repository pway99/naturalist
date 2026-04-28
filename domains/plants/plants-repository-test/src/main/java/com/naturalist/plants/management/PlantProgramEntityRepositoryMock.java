package com.naturalist.plants.management;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantName;

import java.util.List;

public class PlantProgramEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantProgramName, PlantProgram, PlantProgramTestEntitySource>
        implements PlantProgramRepository.PlantProgramEntityRepository {

    protected PlantProgramEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PlantProgramName> getAllPlantProgramNames() {
        return testEntitySource().entityStream()
                .map(PlantProgram::name)
                .toList();
    }

    @Override
    public List<PlantProgram> getByPlantName(PlantName plantName) {
        return testEntitySource().entityStream()
                .filter(p -> p.plantName().equals(plantName))
                .toList();
    }
}
