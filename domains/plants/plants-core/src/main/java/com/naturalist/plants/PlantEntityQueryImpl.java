package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantEntityCollections.PlantSpeciesCollection;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@DomainService
class PlantEntityQueryImpl
        extends AbstractEntityQuery<PlantSpeciesName, PlantSpecies, PlantSpeciesCollection, PlantRepository.PlantEntityRepository>
        implements PlantQuery.PlantEntityQuery {

    private final PlantQuery.PlantGenusEntityQuery genusQuery;

    PlantEntityQueryImpl(PlantRepository.PlantEntityRepository repository,
                         PlantQuery.PlantGenusEntityQuery genusQuery) {
        super(repository);
        this.genusQuery = Objects.requireNonNull(genusQuery, "genusQuery");
    }

    @Override
    public PlantSpeciesCollection findByNameSet(Set<PlantSpeciesName> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();
        return PlantSpeciesCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public PlantSpeciesCollection forGenusName(PlantGenusName genusName) {
        observer().arguments("forGenusName", i -> i.entityName(genusName, "genusName"))
                .throwWhenInvalid();
        return PlantSpeciesCollection.of(repository().getByGenusName(genusName));
    }

    @Override
    public PlantSpeciesCollection forFamilyName(PlantFamilyName familyName) {
        observer().arguments("forFamilyName", i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        List<PlantSpecies> species = genusQuery.forFamilyName(familyName).stream()
                .flatMap(genus -> forGenusName(genus.name()).stream())
                .toList();
        return PlantSpeciesCollection.of(species);
    }
}
