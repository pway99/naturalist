package com.naturalist.plants.management;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantRankName;

import java.util.Set;

@DomainService
class PlantProgramQueryImpl
        extends AbstractEntityQuery<PlantProgramName, PlantProgram, PlantProgramCollection, PlantProgramRepository>
        implements PlantProgramQuery {

    PlantProgramQueryImpl(PlantProgramRepository repository) {
        super(repository);
    }

    @Override
    public PlantProgramCollection findByNameSet(Set<PlantProgramName> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();
        return PlantProgramCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public PlantProgramCollection forPlantName(PlantRankName plantName) {
        observer().arguments("forPlantName", i -> i
                        .identifier(plantName, "plantName"))
                .throwWhenInvalid();
        return PlantProgramCollection.of(repository().getByPlantName(plantName));
    }
}
