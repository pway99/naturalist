package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.compound.CompoundEntityCollections.CompoundCollection;
import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.ddd.EntityNameSet;

import java.util.Set;

class CompoundEntityQueryImpl
        extends AbstractEntityQuery<CompoundName, Compound, CompoundCollection, CompoundRepository.CompoundEntityRepository>
        implements CompoundQuery.CompoundEntityQuery {

    CompoundEntityQueryImpl(CompoundRepository.CompoundEntityRepository repository) {
        super(repository);
    }

    @Override
    public CompoundCollection findByNameSet(Set<CompoundName> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();

        return CompoundCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public EntityNameSet<CompoundName> allCompoundNames() {
        return EntityNameSet.of(repository().getAllCompoundNames());
    }
}
