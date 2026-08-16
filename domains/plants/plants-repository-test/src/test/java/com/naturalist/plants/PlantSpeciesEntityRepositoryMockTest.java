package com.naturalist.plants;

class PlantSpeciesEntityRepositoryMockTest implements PlantSpeciesEntityRepositoryTest {
    @Override
    public PlantRepository.PlantEntityRepository repository() {
        return new PlantSpeciesEntityRepositoryMock(db);
    }
}
