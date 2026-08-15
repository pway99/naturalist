package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class PlantEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantName, Plant, PlantTestEntitySource>
        implements PlantRepository.PlantEntityRepository {

    PlantEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
