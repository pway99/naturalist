package com.naturalist.chemistry.compound.depiction;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;
import java.util.Optional;

class DepictionEntityRepositoryMock
        extends AbstractTestEntityRepository<DepictionId, CompoundDepiction, CompoundDepictionTestEntitySource>
        implements DepictionRepository.DepictionEntityRepository {

    protected DepictionEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Optional<CompoundDepiction> getByCompoundName(CompoundName compoundName) {
        observer().arguments("getByCompoundName", i -> i
                        .identifier(compoundName, "compoundName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(depiction -> depiction.compoundName().equals(compoundName))
                .findFirst();
    }

    @Override
    public List<CompoundName> getAllDepictedCompoundNames() {
        return testEntitySource().entityStream()
                .map(CompoundDepiction::compoundName)
                .toList();
    }
}
