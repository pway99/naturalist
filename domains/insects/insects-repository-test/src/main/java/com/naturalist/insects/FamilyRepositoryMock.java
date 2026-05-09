package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class FamilyRepositoryMock
        extends AbstractTestEntityRepository<InsectFamilyName, InsectFamily, InsectFamilyTestEntitySource>
        implements InsectRepository.FamilyRepository {

    FamilyRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
