package com.naturalist.insects;

class InsectObservationRepositoryMockTest implements InsectObservationEntityRepositoryTest {
    @Override
    public InsectRepository.ObservationRepository repository() {
        return new InsectObservationRepositoryMock(db);
    }
}
