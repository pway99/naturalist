package com.naturalist.soil.observation;

import com.naturalist.data.EntityQuery;

/**
 * Read port for {@link ReportedRecommendation}: by analysis ({@code forLabAnalysisId} — one
 * report's advice) and by input ({@code forInputName} — one input's advice over time).
 */
public interface ReportedRecommendationQuery
        extends EntityQuery<ReportedRecommendationId, ReportedRecommendation, ReportedRecommendationCollection> {

    ReportedRecommendationCollection forLabAnalysisId(LabAnalysisId labAnalysisId);

    ReportedRecommendationCollection forInputName(RecommendedInputName inputName);
}
