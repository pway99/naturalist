package com.naturalist.insects.lifestage;

class InsectLifeStageEntityRepositoryMockTest implements InsectLifeStageEntityRepositoryTest {
    @Override
    public LifeStageRepository.LifeStageEntityRepository repository() {
        return new InsectLifeStageEntityRepositoryMock(db);
    }
}
