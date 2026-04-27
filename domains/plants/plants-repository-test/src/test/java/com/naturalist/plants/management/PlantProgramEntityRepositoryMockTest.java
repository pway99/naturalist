package com.naturalist.plants.management;

class PlantProgramEntityRepositoryMockTest implements PlantProgramEntityRepositoryTest {
    @Override
    public PlantProgramRepository.PlantProgramEntityRepository repository() {
        return new PlantProgramEntityRepositoryMock(db);
    }
}
