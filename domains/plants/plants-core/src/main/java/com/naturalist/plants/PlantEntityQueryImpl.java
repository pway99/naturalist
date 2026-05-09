package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantEntityCollections.PlantCollection;

import java.util.Set;

@DomainService
class PlantEntityQueryImpl
        extends AbstractEntityQuery<PlantName, Plant, PlantCollection, PlantRepository.PlantEntityRepository>
        implements PlantQuery.PlantEntityQuery {

    PlantEntityQueryImpl(PlantRepository.PlantEntityRepository repository) {
        super(repository);
    }

    @Override
    public PlantCollection findByNameSet(Set<PlantName> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();
        return PlantCollection.of(repository().getByEntityNameSet(names));
    }
}
