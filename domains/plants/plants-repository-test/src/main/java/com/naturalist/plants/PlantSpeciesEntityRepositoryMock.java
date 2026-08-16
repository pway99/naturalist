package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class PlantSpeciesEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantSpeciesName, PlantSpecies, PlantSpeciesTestEntitySource>
        implements PlantRepository.PlantEntityRepository {

    PlantSpeciesEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
