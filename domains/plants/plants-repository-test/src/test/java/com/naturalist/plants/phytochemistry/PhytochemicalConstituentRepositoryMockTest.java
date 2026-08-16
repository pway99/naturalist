package com.naturalist.plants.phytochemistry;

class PhytochemicalConstituentRepositoryMockTest implements PhytochemicalConstituentRepositoryTest {
    @Override
    public PhytochemicalConstituentRepository repository() {
        return new PhytochemicalConstituentRepositoryMock(db);
    }
}
