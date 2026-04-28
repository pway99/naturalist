package com.naturalist.plants.heritage;

class SeedLineageEntityRepositoryMockTest implements SeedLineageEntityRepositoryTest {
    @Override
    public SeedLineageRepository.SeedLineageEntityRepository repository() {
        return new SeedLineageEntityRepositoryMock(db);
    }
}
