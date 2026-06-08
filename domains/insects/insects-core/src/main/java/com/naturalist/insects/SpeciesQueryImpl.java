package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@DomainService
class SpeciesQueryImpl
        extends AbstractEntityQuery<
        InsectSpeciesName,
        InsectSpecies,
        InsectEntityCollections.SpeciesCollection,
        InsectRepository.SpeciesRepository>
        implements InsectQuery.SpeciesQuery {

    private final InsectQuery.GenusQuery genusQuery;

    SpeciesQueryImpl(InsectRepository.SpeciesRepository repository,
                     InsectQuery.GenusQuery genusQuery) {
        super(repository);
        this.genusQuery = Objects.requireNonNull(genusQuery, "genusQuery");
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
        List<InsectSpecies> species = genusQuery.forFamilyName(familyName).stream()
                .flatMap(genus -> forGenusName(genus.name()).stream())
                .toList();
        return InsectEntityCollections.SpeciesCollection.of(species);
    }
}
