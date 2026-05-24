package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.taxonomy.TaxonomicGenus;

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
    public InsectEntityCollections.SpeciesCollection forGenusEpithet(TaxonomicGenus genusEpithet) {
        observer().arguments("forGenusEpithet",
                        i -> i.namedValue(genusEpithet, "genusEpithet"))
                .throwWhenInvalid();
        return InsectEntityCollections.SpeciesCollection.of(
                repository().getByGenusEpithet(genusEpithet));
    }
}
