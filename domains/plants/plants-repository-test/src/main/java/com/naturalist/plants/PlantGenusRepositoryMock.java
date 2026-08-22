package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;

class PlantGenusRepositoryMock
        extends AbstractTestEntityRepository<PlantGenusName, PlantGenus, PlantGenusTestEntitySource>
        implements PlantRepository.GenusRepository {

    PlantGenusRepositoryMock(NaturalistDatabase naturalistDatabase) {
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
