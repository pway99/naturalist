package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

class InsectOrderRepositoryMock
        extends AbstractTestEntityRepository<InsectOrderName, InsectOrder, InsectOrderTestEntitySource>
        implements InsectRepository.OrderRepository {

    InsectOrderRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
