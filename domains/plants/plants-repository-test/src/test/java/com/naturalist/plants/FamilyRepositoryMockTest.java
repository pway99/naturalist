package com.naturalist.plants;

class FamilyRepositoryMockTest implements FamilyRepositoryTest {
    @Override
    public PlantRepository.FamilyRepository repository() {
        return new FamilyRepositoryMock(db);
    }
}
