package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.compound.CompoundEntityCollections.CompoundCollection;
import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.ddd.EntityNameSet;
import com.naturalist.infrastructure.DomainService;

import java.util.Set;

@DomainService
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
