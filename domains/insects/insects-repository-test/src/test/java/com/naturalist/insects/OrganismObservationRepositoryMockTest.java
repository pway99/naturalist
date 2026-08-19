package com.naturalist.insects;

class OrganismObservationRepositoryMockTest implements OrganismObservationEntityRepositoryTest {
    @Override
    public InsectRepository.FieldObservationRepository repository() {
        return new OrganismObservationRepositoryMock(db);
    }
}
