package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.compound.CompoundEntityCollections.DepictionCollection;
import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.ddd.EntityNameSet;

import java.util.Optional;
import java.util.Set;

class DepictionQueryImpl
        extends AbstractEntityQuery<DepictionId, CompoundDepiction, DepictionCollection, CompoundRepository.DepictionRepository>
        implements CompoundQuery.DepictionQuery {

    DepictionQueryImpl(CompoundRepository.DepictionRepository repository) {
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
