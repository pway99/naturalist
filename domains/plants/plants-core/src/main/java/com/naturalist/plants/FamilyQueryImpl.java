package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantEntityCollections.FamilyCollection;

import java.util.Set;

@DomainService
class FamilyQueryImpl
        extends AbstractEntityQuery<
        PlantFamilyName,
        PlantFamily,
        FamilyCollection,
        PlantRepository.FamilyRepository>
        implements PlantQuery.FamilyQuery {

    FamilyQueryImpl(PlantRepository.FamilyRepository repository) {
        super(repository);
    }

    @Override
    public FamilyCollection findByNameSet(Set<PlantFamilyName> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return FamilyCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public FamilyCollection forOrderName(PlantOrderName orderName) {
        observer().arguments("forOrderName", i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        return FamilyCollection.of(repository().getByOrderName(orderName));
    }
}
