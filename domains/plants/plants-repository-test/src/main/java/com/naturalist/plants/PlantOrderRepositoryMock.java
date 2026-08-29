package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

@MockDomainService
class PlantOrderRepositoryMock
        extends AbstractTestEntityRepository<PlantOrderName, PlantOrder, PlantOrderTestEntitySource>
        implements PlantRepository.OrderRepository {

    PlantOrderRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
