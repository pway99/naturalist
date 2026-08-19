package com.naturalist.plants;

class PlantObservationRepositoryMockTest implements PlantObservationRepositoryTest {
    @Override
    public PlantRepository.PlantObservationRepository repository() {
        return new PlantObservationRepositoryMock(db);
    }
}
