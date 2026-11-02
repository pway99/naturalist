package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;
import java.util.Set;

@MockDomainService
class InsectGenusRepositoryMock
        extends AbstractTestEntityRepository<InsectGenusName, InsectGenusInfo, InsectGenusTestEntitySource>
        implements InsectRepository.GenusRepository {

    InsectGenusRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectGenusInfo> getByFamilyName(InsectFamilyName familyName) {
        observer().arguments("getByFamilyName",
                        i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(g -> familyName.equals(g.familyName()))
                .toList();
    }

    @Override
    public List<InsectGenusInfo> getByFamilyNames(Set<InsectFamilyName> familyNames) {
        observer().arguments("getByFamilyNames",
                        i -> i.entityNameCollection(familyNames, "familyNames"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(g -> familyNames.contains(g.familyName()))
                .toList();
    }
}
