package com.naturalist.garden;

class PlantingEntityRepositoryMockTest implements PlantingEntityRepositoryTest {

    @Override
    public PlantingRepository repository() {
        return new PlantingEntityRepositoryMock(db);
    }
}
