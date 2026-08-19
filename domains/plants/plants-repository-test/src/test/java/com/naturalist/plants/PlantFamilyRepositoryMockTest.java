package com.naturalist.plants;

class PlantFamilyRepositoryMockTest implements PlantFamilyRepositoryTest {
    @Override
    public PlantRepository.FamilyRepository repository() {
        return new PlantFamilyRepositoryMock(db);
    }
}
