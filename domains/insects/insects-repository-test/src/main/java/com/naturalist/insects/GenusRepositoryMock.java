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
    public List<InsectGenusName> getAllGenusNames() {
        return testEntitySource().entityStream()
                .map(InsectGenus::name)
                .toList();
    }
}
