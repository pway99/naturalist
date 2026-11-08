package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.Optional;

@MockDomainService
class PlantEcologicalRoleRepositoryMock
        extends AbstractTestEntityRepository<PlantEcologicalRoleId, PlantEcologicalRole,
        PlantEcologicalRoleTestEntitySource>
        implements PlantRepository.EcologicalRoleRepository {

    PlantEcologicalRoleRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Optional<PlantEcologicalRole> getByPlantName(PlantRankName plantName) {
        MethodObserver getByPlantName = observer().forMethod("getByPlantName");
        getByPlantName.arguments(i -> i.identifier(plantName, "plantName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(role -> plantName.equals(role.plantName()))
                .findFirst();
        getByPlantName.state(s -> s.namedEntityOrNull(result.orElse(null), "result")).observe(Level.WARN);
        return result;
    }
}
