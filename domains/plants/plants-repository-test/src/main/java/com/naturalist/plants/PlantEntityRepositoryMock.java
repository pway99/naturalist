package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

public class PlantEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantName, Plant, PlantTestEntitySource>
        implements PlantRepository.PlantEntityRepository {

    protected PlantEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
