package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@DomainService
class InsectSpeciesQueryImpl
        extends AbstractEntityQuery<
        InsectSpeciesName,
        InsectSpecies,
        InsectEntityCollections.SpeciesCollection,
        InsectRepository.SpeciesRepository>
        implements InsectQuery.SpeciesQuery {

    private final InsectQuery.GenusQuery genusQuery;

    InsectSpeciesQueryImpl(InsectRepository.SpeciesRepository repository,
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
        Set<InsectGenusName> genusNames = genusQuery.forFamilyName(familyName).stream()
                .map(InsectGenus::name)
                .collect(Collectors.toSet());
        return forGenusNames(genusNames);
    }

    @Override
    public InsectEntityCollections.SpeciesCollection forGenusNames(Set<InsectGenusName> genusNames) {
        observer().arguments("forGenusNames",
                        i -> i.entityNameCollection(genusNames, "genusNames"))
                .throwWhenInvalid();
        return InsectEntityCollections.SpeciesCollection.of(
                repository().getByGenusNames(genusNames));
    }
}
