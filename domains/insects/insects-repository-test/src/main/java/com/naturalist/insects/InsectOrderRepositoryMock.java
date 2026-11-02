package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

@MockDomainService
class InsectOrderRepositoryMock
        extends AbstractTestEntityRepository<InsectOrderName, InsectOrderInfo, InsectOrderTestEntitySource>
        implements InsectRepository.OrderRepository {

    InsectOrderRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
