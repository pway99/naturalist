package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.Optional;

@DomainService
class PlantEcologicalRoleEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantEcologicalRoleId, PlantEcologicalRole,
        PlantEcologicalRoleTestEntitySource>
        implements PlantRepository.PlantEcologicalRoleEntityRepository {

    PlantEcologicalRoleEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Optional<PlantEcologicalRole> getByPlantName(PlantRankName plantName) {
        observer().arguments("getByPlantName", i -> i.identifier(plantName, "plantName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(role -> plantName.equals(role.plantName()))
                .findFirst();
    }
}
