package com.naturalist.insects;

class InsectObservationRepositoryMockTest implements InsectObservationEntityRepositoryTest {
    @Override
    public InsectRepository.InsectObservationRepository repository() {
        return new InsectObservationRepositoryMock(db);
    }
}
