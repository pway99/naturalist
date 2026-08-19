package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantEntityCollections.OrderCollection;

import java.util.Set;

@DomainService
class PlantOrderQueryImpl
        extends AbstractEntityQuery<
        PlantOrderName,
        PlantOrder,
        OrderCollection,
        PlantRepository.OrderRepository>
        implements PlantQuery.OrderQuery {

    PlantOrderQueryImpl(PlantRepository.OrderRepository repository) {
        super(repository);
    }

    @Override
    public OrderCollection findByNameSet(Set<PlantOrderName> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return OrderCollection.of(repository().getByEntityNameSet(names));
    }
}
