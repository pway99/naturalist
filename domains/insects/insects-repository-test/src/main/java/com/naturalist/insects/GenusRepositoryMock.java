package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class GenusRepositoryMock
        extends AbstractTestEntityRepository<InsectGenusName, InsectGenus, InsectGenusTestEntitySource>
        implements InsectRepository.GenusRepository {

    GenusRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectGenus> getByFamilyName(InsectFamilyName familyName) {
        observer().arguments("getByFamilyName",
                        i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(g -> familyName.equals(g.familyName()))
                .toList();
    }

    @Override
    public List<InsectGenus> getByOrderName(InsectOrderName orderName) {
        observer().arguments("getByOrderName",
                        i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(g -> orderName.equals(g.orderName()))
                .toList();
    }
}
