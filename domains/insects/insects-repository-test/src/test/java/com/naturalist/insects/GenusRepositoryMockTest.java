package com.naturalist.insects;

class GenusRepositoryMockTest implements GenusRepositoryTest {
    @Override
    public InsectRepository.GenusRepository repository() {
        return new GenusRepositoryMock(db);
    }
}
