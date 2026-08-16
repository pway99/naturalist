package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantEntityCollections.GenusCollection;

import java.util.Set;

@DomainService
class GenusQueryImpl
        extends AbstractEntityQuery<
        PlantGenusName,
        PlantGenus,
        GenusCollection,
        PlantRepository.GenusRepository>
        implements PlantQuery.GenusQuery {

    GenusQueryImpl(PlantRepository.GenusRepository repository) {
        super(repository);
    }

    @Override
    public GenusCollection findByNameSet(Set<PlantGenusName> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return GenusCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public GenusCollection forFamilyName(PlantFamilyName familyName) {
        observer().arguments("forFamilyName", i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        return GenusCollection.of(repository().getByFamilyName(familyName));
    }
}
