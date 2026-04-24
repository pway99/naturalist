package com.naturalist.insects.lifestage;

class LifeStageEntityRepositoryMockTest implements LifeStageEntityRepositoryTest {
    @Override
    public LifeStageRepository.LifeStageEntityRepository repository() {
        return new LifeStageEntityRepositoryMock(db);
    }
}
