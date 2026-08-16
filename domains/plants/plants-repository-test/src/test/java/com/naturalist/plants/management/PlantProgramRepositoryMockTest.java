package com.naturalist.plants.management;

class PlantProgramRepositoryMockTest implements PlantProgramRepositoryTest {
    @Override
    public PlantProgramRepository repository() {
        return new PlantProgramRepositoryMock(db);
    }
}
