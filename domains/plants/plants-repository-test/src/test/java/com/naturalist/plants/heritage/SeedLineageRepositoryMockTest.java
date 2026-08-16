package com.naturalist.plants.heritage;

class SeedLineageRepositoryMockTest implements SeedLineageRepositoryTest {
    @Override
    public SeedLineageRepository repository() {
        return new SeedLineageRepositoryMock(db);
    }
}
