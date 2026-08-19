package com.naturalist.plants;

class PlantObservationRepositoryMockTest implements PlantObservationRepositoryTest {
    @Override
    public PlantRepository.ObservationRepository repository() {
        return new PlantObservationRepositoryMock(db);
    }
}
