package com.naturalist.soil.observation;

class ReportedRecommendationEntityRepositoryMockTest implements ReportedRecommendationEntityRepositoryTest {

    @Override
    public ReportedRecommendationRepository repository() {
        return new ReportedRecommendationEntityRepositoryMock(db);
    }
}
