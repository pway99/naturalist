package com.naturalist.plants.cultivar;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantName;

import java.util.List;

@DomainService
class CultivarEntityRepositoryMock
        extends AbstractTestEntityRepository<CultivarName, Cultivar, CultivarTestEntitySource>
        implements CultivarRepository.CultivarEntityRepository {

    CultivarEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<Cultivar> getByPlantName(PlantName plantName) {
        observer().arguments("getByPlantName",
                        i -> i.entityName(plantName, "plantName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(c -> plantName.equals(c.plantName()))
                .toList();
    }
}
