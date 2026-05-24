package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
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
    public InsectEntityCollections.SpeciesCollection forGenusName(InsectGenusName genusName) {
        observer().arguments("forGenusName",
                        i -> i.entityName(genusName, "genusName"))
                .throwWhenInvalid();
        return InsectEntityCollections.SpeciesCollection.of(
                repository().getByGenusName(genusName));
    }

    @Override
    public InsectEntityCollections.SpeciesCollection forFamilyName(InsectFamilyName familyName) {
        observer().arguments("forFamilyName",
                        i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        return InsectEntityCollections.SpeciesCollection.of(
                repository().getByFamilyName(familyName));
    }
}
