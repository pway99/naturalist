package com.naturalist.soil.observation;

import com.naturalist.data.EntityRepository;

import java.util.List;

/**
 * The single repository for every {@link ReportedRecommendation} across all analyses.
 * Package-private (ADR-020). {@code getByLabAnalysisId} gathers one report's advice;
 * {@code getByInputName} follows one input across reports — the series that shows whether a lab
 * kept recommending the same thing.
 */
interface ReportedRecommendationRepository
        extends EntityRepository<ReportedRecommendationId, ReportedRecommendation> {

    List<ReportedRecommendation> getByLabAnalysisId(LabAnalysisId labAnalysisId);

    List<ReportedRecommendation> getByInputName(RecommendedInputName inputName);
}
