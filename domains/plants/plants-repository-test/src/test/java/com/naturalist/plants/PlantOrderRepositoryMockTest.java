package com.naturalist.plants;

class PlantOrderRepositoryMockTest implements PlantOrderRepositoryTest {
    @Override
    public PlantRepository.OrderRepository repository() {
        return new PlantOrderRepositoryMock(db);
    }
}
