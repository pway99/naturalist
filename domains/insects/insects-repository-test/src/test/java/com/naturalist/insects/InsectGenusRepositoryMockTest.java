package com.naturalist.insects;

class InsectGenusRepositoryMockTest implements InsectGenusRepositoryTest {
    @Override
    public InsectRepository.GenusRepository repository() {
        return new InsectGenusRepositoryMock(db);
    }
}
