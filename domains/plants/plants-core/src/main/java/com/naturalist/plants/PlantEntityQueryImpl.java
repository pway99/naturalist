package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantEntityCollections.PlantSpeciesCollection;

import java.util.Set;

@DomainService
class PlantEntityQueryImpl
        extends AbstractEntityQuery<PlantSpeciesName, PlantSpecies, PlantSpeciesCollection, PlantRepository.PlantEntityRepository>
        implements PlantQuery.PlantEntityQuery {

    PlantEntityQueryImpl(PlantRepository.PlantEntityRepository repository) {
        super(repository);
    }

    @Override
    public PlantSpeciesCollection findByNameSet(Set<PlantSpeciesName> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();
        return PlantSpeciesCollection.of(repository().getByEntityNameSet(names));
    }
}
