package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;

/**
 * Wiring seam exposing a {@link ReportedRecommendationQuery} from a {@link NaturalistDatabase},
 * mirroring {@link ReportedOptimumQueries}.
 */
public final class ReportedRecommendationQueries {

    private ReportedRecommendationQueries() {
    }

    public static ReportedRecommendationQuery create(NaturalistDatabase database) {
        return new ReportedRecommendationQueryImpl(new ReportedRecommendationEntityRepositoryMock(database));
    }
}
