package com.naturalist.soil.observation;

class LabAnalysisInfoEntityRepositoryMockTest implements LabAnalysisInfoEntityRepositoryTest {

    @Override
    public LabAnalysisInfoRepository repository() {
        return new LabAnalysisInfoEntityRepositoryMock(db);
    }
}
