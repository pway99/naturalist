package com.naturalist.insects;

class SpeciesRepositoryMockTest implements SpeciesRepositoryTest {
    @Override
    public InsectRepository.SpeciesRepository repository() {
        return new SpeciesRepositoryMock(db);
    }
}
