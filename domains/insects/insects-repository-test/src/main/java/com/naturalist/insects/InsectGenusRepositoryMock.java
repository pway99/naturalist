package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class InsectGenusRepositoryMock
        extends AbstractTestEntityRepository<InsectGenusName, InsectGenus, InsectGenusTestEntitySource>
        implements InsectRepository.GenusRepository {

    InsectGenusRepositoryMock(NaturalistDatabase naturalistDatabase) {
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
}
