package com.naturalist.plants;

class PlantFamilyEntityRepositoryMockTest implements PlantFamilyEntityRepositoryTest {
    @Override
    public PlantRepository.PlantFamilyEntityRepository repository() {
        return new PlantFamilyEntityRepositoryMock(db);
    }
}
