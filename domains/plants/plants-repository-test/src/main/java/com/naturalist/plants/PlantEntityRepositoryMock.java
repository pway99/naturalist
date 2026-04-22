package com.naturalist.plants;

import com.naturalist.data.AbstractTestNamedEntityRepository;
import com.naturalist.data.NaturalistDatabase;

public class PlantEntityRepositoryMock
        extends AbstractTestNamedEntityRepository<PlantName, Plant, PlantTestEntitySource>
        implements PlantRepository.PlantEntityRepository {

    protected PlantEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
