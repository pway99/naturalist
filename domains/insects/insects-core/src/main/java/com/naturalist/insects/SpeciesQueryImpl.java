package com.naturalist.insects;

import com.naturalist.data.AbstractNamedEntityQuery;
import com.naturalist.ddd.EntityNameSet;

import java.util.Set;

class SpeciesQueryImpl
        extends AbstractNamedEntityQuery<
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
}
