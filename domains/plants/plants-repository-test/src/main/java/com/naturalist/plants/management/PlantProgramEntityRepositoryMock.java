package com.naturalist.plants.management;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantSpeciesName;

import java.util.List;

@DomainService
class PlantProgramEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantProgramName, PlantProgram, PlantProgramTestEntitySource>
        implements PlantProgramRepository.PlantProgramEntityRepository {

    PlantProgramEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PlantProgram> getByPlantName(PlantSpeciesName plantName) {
        observer().arguments("getByPlantName",
                        i -> i.entityName(plantName, "plantName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(p -> plantName.equals(p.plantName()))
                .toList();
    }
}
