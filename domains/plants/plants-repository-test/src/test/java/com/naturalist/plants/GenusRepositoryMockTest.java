package com.naturalist.plants;

class GenusRepositoryMockTest implements GenusRepositoryTest {
    @Override
    public PlantRepository.GenusRepository repository() {
        return new GenusRepositoryMock(db);
    }
}
