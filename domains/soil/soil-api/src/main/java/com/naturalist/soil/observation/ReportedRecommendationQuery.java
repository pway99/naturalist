package com.naturalist.soil.observation;

import com.naturalist.data.EntityQuery;

import java.util.Set;

/**
 * Read port for {@link ReportedRecommendation}: by analysis ({@code forLabAnalysisId} — one
 * report's advice) and by input ({@code forInputName} — one input's advice over time).
 */
public interface ReportedRecommendationQuery
        extends EntityQuery<ReportedRecommendationId, ReportedRecommendation, ReportedRecommendationCollection> {

    ReportedRecommendationCollection forLabAnalysisId(LabAnalysisId labAnalysisId);

    /** Batched sibling of {@link #forLabAnalysisId} — every analysis's advice in one call. */
    ReportedRecommendationCollection forLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds);

    ReportedRecommendationCollection forInputName(RecommendedInputName inputName);
}
