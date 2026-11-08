package com.naturalist.chemistry.compound;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;
import java.util.Optional;

@MockDomainService
class DepictionEntityRepositoryMock
        extends AbstractTestEntityRepository<DepictionId, CompoundDepiction, CompoundDepictionTestEntitySource>
        implements CompoundRepository.DepictionRepository {

    protected DepictionEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Optional<CompoundDepiction> getByCompoundName(CompoundName compoundName) {
        MethodObserver getByCompoundName = observer().forMethod("getByCompoundName");
        getByCompoundName.arguments(i -> i
                        .identifier(compoundName, "compoundName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(depiction -> depiction.compoundName().equals(compoundName))
                .findFirst();
        getByCompoundName.state(s -> s.namedEntityOrNull(result.orElse(null), "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<CompoundName> getAllDepictedCompoundNames() {
        MethodObserver getAllDepictedCompoundNames = observer().forMethod("getAllDepictedCompoundNames");
        var result = testEntitySource().entityStream()
                .map(CompoundDepiction::compoundName)
                .toList();
        getAllDepictedCompoundNames.state(s -> s.entityNameCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
