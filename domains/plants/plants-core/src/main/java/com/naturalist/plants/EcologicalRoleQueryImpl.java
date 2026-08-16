package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantEntityCollections.EcologicalRoleCollection;

import java.util.Optional;
import java.util.Set;

@DomainService
class EcologicalRoleQueryImpl
        extends AbstractEntityQuery<
        PlantEcologicalRoleId,
        PlantEcologicalRole,
        EcologicalRoleCollection,
        PlantRepository.EcologicalRoleRepository>
        implements PlantQuery.EcologicalRoleQuery {

    EcologicalRoleQueryImpl(PlantRepository.EcologicalRoleRepository repository) {
        super(repository);
    }

    @Override
    public EcologicalRoleCollection findByNameSet(Set<PlantEcologicalRoleId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return EcologicalRoleCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public Optional<PlantEcologicalRole> forPlantName(PlantRankName plantName) {
        observer().arguments("forPlantName", i -> i.identifier(plantName, "plantName"))
                .throwWhenInvalid();
        return repository().getByPlantName(plantName);
    }
}
