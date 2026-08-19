package com.naturalist.plants;

class OrganismObservationRepositoryMockTest implements OrganismObservationRepositoryTest {
    @Override
    public PlantRepository.FieldObservationRepository repository() {
        return new OrganismObservationRepositoryMock(db);
    }
}
