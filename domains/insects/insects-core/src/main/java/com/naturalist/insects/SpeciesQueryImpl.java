package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.ddd.EntityNameSet;
import com.naturalist.infrastructure.DomainService;

import java.util.Set;

@DomainService
class SpeciesQueryImpl
        extends AbstractEntityQuery<
                        InsectSpeciesName,
                        InsectSpecies,
                        InsectEntityCollections.SpeciesCollection,
                        InsectRepository.SpeciesRepository>
        implements InsectQuery.SpeciesQuery {

    SpeciesQueryImpl(InsectRepository.SpeciesRepository repository) {
        super(repository);
    }

    @Override
    public InsectEntityCollections.SpeciesCollection findByNameSet(Set<InsectSpeciesName> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return InsectEntityCollections.SpeciesCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public EntityNameSet<InsectSpeciesName> allSpeciesNames() {
        return EntityNameSet.of(repository().getAllSpeciesNames());
    }

    @Override
    public InsectEntityCollections.SpeciesCollection getByFunctionalGuild(FunctionalGuild functionalGuild) {
        observer().arguments("getByFunctionalGuild", i -> i
                .notNull(functionalGuild, "functionalGuild"))
                .throwWhenInvalid();
        return InsectEntityCollections.SpeciesCollection.of(repository().getByFunctionalGuild(functionalGuild));
    }
}
