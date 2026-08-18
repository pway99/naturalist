package com.naturalist.plants;

class PlantImageRepositoryMockTest implements PlantImageRepositoryTest {
    @Override
    public PlantRepository.ImageRepository repository() {
        return new PlantImageRepositoryMock(db);
    }
}
