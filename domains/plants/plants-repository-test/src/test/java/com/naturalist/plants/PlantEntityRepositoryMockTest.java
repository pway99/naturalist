package com.naturalist.plants;

class PlantEntityRepositoryMockTest implements PlantEntityRepositoryTest {
    @Override
    public PlantRepository.PlantEntityRepository repository() {
        return new PlantEntityRepositoryMock(db);
    }
}
