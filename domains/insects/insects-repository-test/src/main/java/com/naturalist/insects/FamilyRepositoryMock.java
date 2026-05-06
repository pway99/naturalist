package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class FamilyRepositoryMock
        extends AbstractTestEntityRepository<InsectFamilyName, InsectFamily, InsectFamilyTestEntitySource>
        implements InsectRepository.FamilyRepository {

    FamilyRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectFamilyName> getAllFamilyNames() {
        return testEntitySource().entityStream()
                .map(InsectFamily::name)
                .toList();
    }
}
