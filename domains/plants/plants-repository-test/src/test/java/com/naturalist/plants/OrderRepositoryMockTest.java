package com.naturalist.plants;

class OrderRepositoryMockTest implements OrderRepositoryTest {
    @Override
    public PlantRepository.OrderRepository repository() {
        return new OrderRepositoryMock(db);
    }
}
