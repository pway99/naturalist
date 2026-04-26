package com.naturalist.chemistry.compound;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;

class CompoundEntityRepositoryMock
        extends AbstractTestEntityRepository<CompoundName, Compound, CompoundTestEntitySource>
        implements CompoundRepository.CompoundEntityRepository {

    protected CompoundEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<CompoundName> getAllCompoundNames() {
        return testEntitySource().entityStream()
                .map(Compound::name)
                .toList();
    }
}
