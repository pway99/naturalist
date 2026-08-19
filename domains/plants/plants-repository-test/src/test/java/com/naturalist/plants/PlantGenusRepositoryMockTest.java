package com.naturalist.plants;

class PlantGenusRepositoryMockTest implements PlantGenusRepositoryTest {
    @Override
    public PlantRepository.GenusRepository repository() {
        return new PlantGenusRepositoryMock(db);
    }
}
