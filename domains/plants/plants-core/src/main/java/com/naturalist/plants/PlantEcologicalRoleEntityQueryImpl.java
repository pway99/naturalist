package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantEntityCollections.PlantEcologicalRoleCollection;

import java.util.Optional;
import java.util.Set;

@DomainService
class PlantEcologicalRoleEntityQueryImpl
        extends AbstractEntityQuery<
        PlantEcologicalRoleId,
        PlantEcologicalRole,
        PlantEcologicalRoleCollection,
        PlantRepository.PlantEcologicalRoleEntityRepository>
        implements PlantQuery.PlantEcologicalRoleEntityQuery {

    PlantEcologicalRoleEntityQueryImpl(PlantRepository.PlantEcologicalRoleEntityRepository repository) {
        super(repository);
    }

    @Override
    public PlantEcologicalRoleCollection findByNameSet(Set<PlantEcologicalRoleId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return PlantEcologicalRoleCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public Optional<PlantEcologicalRole> forPlantName(PlantRankName plantName) {
        observer().arguments("forPlantName", i -> i.identifier(plantName, "plantName"))
                .throwWhenInvalid();
        return repository().getByPlantName(plantName);
    }
}
