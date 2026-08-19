package com.naturalist.plants.phytochemistry;

class PlantPhytochemicalConstituentRepositoryMockTest implements PlantPhytochemicalConstituentRepositoryTest {
    @Override
    public PhytochemicalConstituentRepository repository() {
        return new PlantPhytochemicalConstituentRepositoryMock(db);
    }
}
