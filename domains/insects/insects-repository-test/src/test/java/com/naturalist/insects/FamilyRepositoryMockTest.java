package com.naturalist.insects;

class FamilyRepositoryMockTest implements FamilyRepositoryTest {
    @Override
    public InsectRepository.FamilyRepository repository() {
        return new FamilyRepositoryMock(db);
    }
}
