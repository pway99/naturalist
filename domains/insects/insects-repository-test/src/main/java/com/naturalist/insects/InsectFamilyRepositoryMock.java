package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;
import java.util.Set;

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

    @Override
    public List<InsectFamily> getByOrderNames(Set<InsectOrderName> orderNames) {
        observer().arguments("getByOrderNames",
                        i -> i.entityNameCollection(orderNames, "orderNames"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(f -> orderNames.contains(f.orderName()))
                .toList();
    }
}
