package com.naturalist.plants;

class PlantFeatureRepositoryMockTest implements PlantFeatureRepositoryTest {
    @Override
    public PlantRepository.FeatureRepository repository() {
        return new PlantFeatureRepositoryMock(db);
    }
}
