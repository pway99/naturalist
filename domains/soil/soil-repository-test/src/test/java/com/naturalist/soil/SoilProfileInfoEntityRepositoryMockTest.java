package com.naturalist.soil;

class SoilProfileInfoEntityRepositoryMockTest implements SoilProfileInfoEntityRepositoryTest {

    @Override
    public SoilProfileInfoRepository repository() {
        return new SoilProfileInfoEntityRepositoryMock(db);
    }
}
