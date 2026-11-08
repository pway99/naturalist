package com.naturalist.plants.management;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.plants.PlantRankName;

import java.util.List;

@MockDomainService
class PlantProgramRepositoryMock
        extends AbstractTestEntityRepository<PlantProgramName, PlantProgram, PlantProgramTestEntitySource>
        implements PlantProgramRepository {

    PlantProgramRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PlantProgram> getByPlantName(PlantRankName plantName) {
        MethodObserver getByPlantName = observer().forMethod("getByPlantName");
        getByPlantName.arguments(i -> i.identifier(plantName, "plantName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(p -> plantName.equals(p.plantName()))
                .toList();
        getByPlantName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
