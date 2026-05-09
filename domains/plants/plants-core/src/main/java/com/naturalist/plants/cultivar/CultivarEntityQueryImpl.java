package com.naturalist.plants.cultivar;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.cultivar.CultivarEntityCollections.CultivarCollection;

import java.util.Set;

@DomainService
class CultivarEntityQueryImpl
        extends AbstractEntityQuery<CultivarName, Cultivar, CultivarCollection, CultivarRepository.CultivarEntityRepository>
        implements CultivarQuery.CultivarEntityQuery {

    CultivarEntityQueryImpl(CultivarRepository.CultivarEntityRepository repository) {
        super(repository);
    }

    @Override
    public CultivarCollection findByNameSet(Set<CultivarName> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();
        return CultivarCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public CultivarCollection forPlantName(PlantName plantName) {
        observer().arguments("forPlantName", i -> i
                        .entityName(plantName, "plantName"))
                .throwWhenInvalid();
        return CultivarCollection.of(repository().getByPlantName(plantName));
    }
}
