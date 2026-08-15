package com.naturalist.soil.observation;

class ReportedOptimumEntityRepositoryMockTest implements ReportedOptimumEntityRepositoryTest {

    @Override
    public ReportedOptimumRepository repository() {
        return new ReportedOptimumEntityRepositoryMock(db);
    }
}
