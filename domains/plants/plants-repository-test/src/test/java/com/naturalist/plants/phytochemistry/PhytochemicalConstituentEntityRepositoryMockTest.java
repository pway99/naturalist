package com.naturalist.plants.phytochemistry;

class PhytochemicalConstituentEntityRepositoryMockTest implements PhytochemicalConstituentEntityRepositoryTest {
    @Override
    public PhytochemicalConstituentRepository.PhytochemicalConstituentEntityRepository repository() {
        return new PhytochemicalConstituentEntityRepositoryMock(db);
    }
}
