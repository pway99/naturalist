package com.naturalist.plants.management;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.ddd.EntityNameSet;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.management.PlantProgramEntityCollections.PlantProgramCollection;

import java.util.Set;

@DomainService
class PlantProgramEntityQueryImpl
        extends AbstractEntityQuery<PlantProgramName, PlantProgram, PlantProgramCollection, PlantProgramRepository.PlantProgramEntityRepository>
        implements PlantProgramQuery.PlantProgramEntityQuery {

    PlantProgramEntityQueryImpl(PlantProgramRepository.PlantProgramEntityRepository repository) {
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
    public EntityNameSet<PlantProgramName> allPlantProgramNames() {
        return EntityNameSet.of(repository().getAllPlantProgramNames());
    }

    @Override
    public PlantProgramCollection forPlantName(PlantName plantName) {
        observer().arguments("forPlantName", i -> i
                        .entityName(plantName, "plantName"))
                .throwWhenInvalid();
        return PlantProgramCollection.of(repository().getByPlantName(plantName));
    }
}
