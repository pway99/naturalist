package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class PlantOrderRepositoryMock
        extends AbstractTestEntityRepository<PlantOrderName, PlantOrder, PlantOrderTestEntitySource>
        implements PlantRepository.OrderRepository {

    PlantOrderRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
