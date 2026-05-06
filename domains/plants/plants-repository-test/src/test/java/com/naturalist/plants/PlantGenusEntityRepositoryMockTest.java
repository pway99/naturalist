package com.naturalist.plants;

class PlantGenusEntityRepositoryMockTest implements PlantGenusEntityRepositoryTest {
    @Override
    public PlantRepository.PlantGenusEntityRepository repository() {
        return new PlantGenusEntityRepositoryMock(db);
    }
}
