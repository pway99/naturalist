package com.naturalist.insects;

import com.naturalist.data.AbstractNamedEntityQuery;

import java.util.Set;

class SpeciesQueryImpl
        extends AbstractNamedEntityQuery<InsectSpeciesName, InsectSpecies, InsectEntityCollections.SpeciesCollection>
        implements InsectQuery.SpeciesQuery {

    SpeciesQueryImpl(InsectRepository.SpeciesRepository repository) {
        super(repository);
    }

    @Override
    public InsectEntityCollections.SpeciesCollection findByNameSet(Set<InsectSpeciesName> names) {
        return InsectEntityCollections.SpeciesCollection.of(repository().getByEntityNameSet(names));
    }
}
