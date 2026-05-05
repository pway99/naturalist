package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class GenusRepositoryMock
        extends AbstractTestEntityRepository<InsectGenusName, InsectGenus, InsectGenusTestEntitySource>
        implements InsectRepository.GenusRepository {

    GenusRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
