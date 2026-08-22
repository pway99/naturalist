package com.naturalist.plants.cultivar;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantSpeciesName;

import java.util.List;

class PlantCultivarRepositoryMock
        extends AbstractTestEntityRepository<CultivarName, Cultivar, PlantCultivarTestEntitySource>
        implements CultivarRepository {

    PlantCultivarRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<Cultivar> getByPlantName(PlantSpeciesName plantName) {
        observer().arguments("getByPlantName",
                        i -> i.entityName(plantName, "plantName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(c -> plantName.equals(c.plantName()))
                .toList();
    }
}
