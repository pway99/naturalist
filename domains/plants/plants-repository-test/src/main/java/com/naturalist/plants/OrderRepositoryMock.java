package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class OrderRepositoryMock
        extends AbstractTestEntityRepository<PlantOrderName, PlantOrder, PlantOrderTestEntitySource>
        implements PlantRepository.OrderRepository {

    OrderRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
