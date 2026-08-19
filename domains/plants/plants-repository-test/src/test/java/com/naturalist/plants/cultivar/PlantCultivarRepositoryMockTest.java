package com.naturalist.plants.cultivar;

class PlantCultivarRepositoryMockTest implements PlantCultivarRepositoryTest {
    @Override
    public CultivarRepository repository() {
        return new PlantCultivarRepositoryMock(db);
    }
}
