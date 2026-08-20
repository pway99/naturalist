package com.naturalist.insects;

class InsectImageRepositoryMockTest implements InsectImageRepositoryTest {
    @Override
    public InsectRepository.ImageRepository repository() {
        return new InsectImageRepositoryMock(db);
    }
}
