package com.naturalist.soil.observation;

class NutrientReadingEntityRepositoryMockTest implements NutrientReadingEntityRepositoryTest {

    @Override
    public NutrientReadingRepository repository() {
        return new NutrientReadingEntityRepositoryMock(db);
    }
}
