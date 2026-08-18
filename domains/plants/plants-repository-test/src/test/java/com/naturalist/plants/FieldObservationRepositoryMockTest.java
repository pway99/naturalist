package com.naturalist.plants;

class FieldObservationRepositoryMockTest implements FieldObservationRepositoryTest {
    @Override
    public PlantRepository.FieldObservationRepository repository() {
        return new FieldObservationRepositoryMock(db);
    }
}
