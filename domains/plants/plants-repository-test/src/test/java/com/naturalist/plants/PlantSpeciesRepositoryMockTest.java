package com.naturalist.plants;

class PlantSpeciesRepositoryMockTest implements PlantSpeciesRepositoryTest {
    @Override
    public PlantRepository.SpeciesRepository repository() {
        return new PlantSpeciesRepositoryMock(db);
    }
}
