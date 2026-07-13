package com.naturalist.insects;

class FieldObservationRepositoryMockTest implements FieldObservationEntityRepositoryTest {
    @Override
    public InsectRepository.FieldObservationRepository repository() {
        return new FieldObservationRepositoryMock(db);
    }
}
