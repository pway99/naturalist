package com.naturalist.insects;

class InsectSpeciesRepositoryMockTest implements InsectSpeciesRepositoryTest {
    @Override
    public InsectRepository.SpeciesRepository repository() {
        return new InsectSpeciesRepositoryMock(db);
    }
}
