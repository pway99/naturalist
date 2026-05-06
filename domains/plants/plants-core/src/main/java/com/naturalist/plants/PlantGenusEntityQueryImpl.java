package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.ddd.EntityNameSet;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantEntityCollections.PlantGenusCollection;

import java.util.Set;

@DomainService
class PlantGenusEntityQueryImpl
        extends AbstractEntityQuery<
        PlantGenusName,
        PlantGenus,
        PlantGenusCollection,
        PlantRepository.PlantGenusEntityRepository>
        implements PlantQuery.PlantGenusEntityQuery {

    PlantGenusEntityQueryImpl(PlantRepository.PlantGenusEntityRepository repository) {
        super(repository);
    }

    @Override
    public PlantGenusCollection findByNameSet(Set<PlantGenusName> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return PlantGenusCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public EntityNameSet<PlantGenusName> allGenusNames() {
        return EntityNameSet.of(repository().getAllGenusNames());
    }
}
