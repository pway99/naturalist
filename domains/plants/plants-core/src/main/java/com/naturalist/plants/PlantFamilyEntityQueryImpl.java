package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.ddd.EntityNameSet;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantEntityCollections.PlantFamilyCollection;

import java.util.Set;

@DomainService
class PlantFamilyEntityQueryImpl
        extends AbstractEntityQuery<
        PlantFamilyName,
        PlantFamily,
        PlantFamilyCollection,
        PlantRepository.PlantFamilyEntityRepository>
        implements PlantQuery.PlantFamilyEntityQuery {

    PlantFamilyEntityQueryImpl(PlantRepository.PlantFamilyEntityRepository repository) {
        super(repository);
    }

    @Override
    public PlantFamilyCollection findByNameSet(Set<PlantFamilyName> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return PlantFamilyCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public EntityNameSet<PlantFamilyName> allFamilyNames() {
        return EntityNameSet.of(repository().getAllFamilyNames());
    }
}
