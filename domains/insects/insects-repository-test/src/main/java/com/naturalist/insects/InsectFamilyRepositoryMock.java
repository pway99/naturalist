package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class InsectFamilyRepositoryMock
        extends AbstractTestEntityRepository<InsectFamilyName, InsectFamily, InsectFamilyTestEntitySource>
        implements InsectRepository.FamilyRepository {

    InsectFamilyRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectFamily> getByOrderName(InsectOrderName orderName) {
        observer().arguments("getByOrderName",
                        i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(f -> orderName.equals(f.orderName()))
                .toList();
    }
}
