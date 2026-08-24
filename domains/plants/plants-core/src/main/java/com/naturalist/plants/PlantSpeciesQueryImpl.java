package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantEntityCollections.SpeciesCollection;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@DomainService
class PlantSpeciesQueryImpl
        extends AbstractEntityQuery<PlantSpeciesName, PlantSpecies, SpeciesCollection, PlantRepository.SpeciesRepository>
        implements PlantQuery.SpeciesQuery {

    private final PlantQuery.GenusQuery genusQuery;

    PlantSpeciesQueryImpl(PlantRepository.SpeciesRepository repository,
                         PlantQuery.GenusQuery genusQuery) {
        super(repository);
        this.genusQuery = Objects.requireNonNull(genusQuery, "genusQuery");
    }

    @Override
    public SpeciesCollection findByNameSet(Set<PlantSpeciesName> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();
        return SpeciesCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public SpeciesCollection forGenusName(PlantGenusName genusName) {
        observer().arguments("forGenusName", i -> i.entityName(genusName, "genusName"))
                .throwWhenInvalid();
        return SpeciesCollection.of(repository().getByGenusName(genusName));
    }

    @Override
    public SpeciesCollection forFamilyName(PlantFamilyName familyName) {
        observer().arguments("forFamilyName", i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        Set<PlantGenusName> genusNames = genusQuery.forFamilyName(familyName).stream()
                .map(PlantGenus::name)
                .collect(Collectors.toSet());
        return forGenusNames(genusNames);
    }

    @Override
    public SpeciesCollection forGenusNames(Set<PlantGenusName> genusNames) {
        observer().arguments("forGenusNames", i -> i.entityNameCollection(genusNames, "genusNames"))
                .throwWhenInvalid();
        return SpeciesCollection.of(repository().getByGenusNames(genusNames));
    }
}
