package com.naturalist.chemistry.compound.depiction;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.ddd.EntityNameSet;
import com.naturalist.infrastructure.DomainService;

import java.util.Optional;
import java.util.Set;

@DomainService
class DepictionQueryImpl
        extends AbstractEntityQuery<DepictionId, CompoundDepiction, DepictionCollection, DepictionRepository.DepictionEntityRepository>
        implements DepictionQuery {

    DepictionQueryImpl(DepictionRepository.DepictionEntityRepository repository) {
        super(repository);
    }

    @Override
    public DepictionCollection findByNameSet(Set<DepictionId> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();

        return DepictionCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public Optional<CompoundDepiction> getByCompoundName(CompoundName compoundName) {
        observer().arguments("getByCompoundName", i -> i
                        .identifier(compoundName, "compoundName"))
                .throwWhenInvalid();

        return repository().getByCompoundName(compoundName);
    }

    @Override
    public EntityNameSet<CompoundName> allDepictedCompounds() {
        return EntityNameSet.of(repository().getAllDepictedCompoundNames());
    }
}
