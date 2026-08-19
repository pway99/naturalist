package com.naturalist.plants.heritage;

class PlantSeedLineageRepositoryMockTest implements PlantSeedLineageRepositoryTest {
    @Override
    public SeedLineageRepository repository() {
        return new PlantSeedLineageRepositoryMock(db);
    }
}
