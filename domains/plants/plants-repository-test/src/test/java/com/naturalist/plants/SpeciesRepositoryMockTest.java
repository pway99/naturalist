package com.naturalist.plants;

class SpeciesRepositoryMockTest implements SpeciesRepositoryTest {
    @Override
    public PlantRepository.SpeciesRepository repository() {
        return new SpeciesRepositoryMock(db);
    }
}
