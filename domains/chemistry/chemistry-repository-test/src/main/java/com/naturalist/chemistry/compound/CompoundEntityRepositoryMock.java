package com.naturalist.chemistry.compound;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

@MockDomainService
class CompoundEntityRepositoryMock
        extends AbstractTestEntityRepository<CompoundName, Compound, CompoundTestEntitySource>
        implements CompoundRepository.CompoundEntityRepository {

    protected CompoundEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
