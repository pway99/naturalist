package com.naturalist.chemistry.compound;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class CompoundEntityRepositoryMock
        extends AbstractTestEntityRepository<CompoundName, Compound, CompoundTestEntitySource>
        implements CompoundRepository.CompoundEntityRepository {

    protected CompoundEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
