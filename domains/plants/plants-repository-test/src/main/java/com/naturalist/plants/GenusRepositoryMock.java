package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class GenusRepositoryMock
        extends AbstractTestEntityRepository<PlantGenusName, PlantGenus, PlantGenusTestEntitySource>
        implements PlantRepository.GenusRepository {

    GenusRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PlantGenus> getByFamilyName(PlantFamilyName familyName) {
        observer().arguments("getByFamilyName",
                        i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(g -> familyName.equals(g.familyName()))
                .toList();
    }
}
