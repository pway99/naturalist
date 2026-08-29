package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

@MockDomainService
class PlantFeatureRepositoryMock
        extends AbstractTestEntityRepository<PlantFeatureId, PlantFeature, PlantFeatureTestEntitySource>
        implements PlantRepository.FeatureRepository {

    PlantFeatureRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
