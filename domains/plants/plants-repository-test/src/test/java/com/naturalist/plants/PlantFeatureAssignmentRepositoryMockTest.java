package com.naturalist.plants;

class PlantFeatureAssignmentRepositoryMockTest implements PlantFeatureAssignmentEntityRepositoryTest {
    @Override
    public PlantRepository.FeatureAssignmentRepository repository() {
        return new PlantFeatureAssignmentRepositoryMock(db);
    }
}
