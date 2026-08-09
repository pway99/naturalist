package com.naturalist.soil.observation;

class SoilPhysicalCharacteristicsEntityRepositoryMockTest
        implements SoilPhysicalCharacteristicsEntityRepositoryTest {

    @Override
    public SoilPhysicalCharacteristicsRepository repository() {
        return new SoilPhysicalCharacteristicsEntityRepositoryMock(db);
    }
}
