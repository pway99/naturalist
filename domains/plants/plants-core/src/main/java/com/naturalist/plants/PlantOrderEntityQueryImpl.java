package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantEntityCollections.PlantOrderCollection;

import java.util.Set;

@DomainService
class PlantOrderEntityQueryImpl
        extends AbstractEntityQuery<
        PlantOrderName,
        PlantOrder,
        PlantOrderCollection,
        PlantRepository.PlantOrderEntityRepository>
        implements PlantQuery.PlantOrderEntityQuery {

    PlantOrderEntityQueryImpl(PlantRepository.PlantOrderEntityRepository repository) {
        super(repository);
    }

    @Override
    public PlantOrderCollection findByNameSet(Set<PlantOrderName> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return PlantOrderCollection.of(repository().getByEntityNameSet(names));
    }
}
