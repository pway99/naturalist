package com.naturalist.insects;

class InsectFamilyRepositoryMockTest implements InsectFamilyRepositoryTest {
    @Override
    public InsectRepository.FamilyRepository repository() {
        return new InsectFamilyRepositoryMock(db);
    }
}
