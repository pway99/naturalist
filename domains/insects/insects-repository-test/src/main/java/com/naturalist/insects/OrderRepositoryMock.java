package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class OrderRepositoryMock
        extends AbstractTestEntityRepository<InsectOrderName, InsectOrder, InsectOrderTestEntitySource>
        implements InsectRepository.OrderRepository {

    OrderRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
