package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class PlantOrderEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantOrderName, PlantOrder, PlantOrderTestEntitySource>
        implements PlantRepository.PlantOrderEntityRepository {

    PlantOrderEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
