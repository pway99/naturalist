package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class FamilyRepositoryMock
        extends AbstractTestEntityRepository<PlantFamilyName, PlantFamily, PlantFamilyTestEntitySource>
        implements PlantRepository.FamilyRepository {

    FamilyRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PlantFamily> getByOrderName(PlantOrderName orderName) {
        observer().arguments("getByOrderName", i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(f -> orderName.equals(f.orderName()))
                .toList();
    }
}
