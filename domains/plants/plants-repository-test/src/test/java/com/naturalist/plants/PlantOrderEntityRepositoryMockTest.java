package com.naturalist.plants;

class PlantOrderEntityRepositoryMockTest implements PlantOrderEntityRepositoryTest {
    @Override
    public PlantRepository.PlantOrderEntityRepository repository() {
        return new PlantOrderEntityRepositoryMock(db);
    }
}
