package com.naturalist.soil.observation;

import com.naturalist.data.EntityRepository;

import java.util.List;
import java.util.Set;

/**
 * The single repository for every {@link ReportedOptimum} across all analyses. Package-private
 * (ADR-020). {@code getByLabAnalysisId} assembles an analysis's printed optima;
 * {@code getByNutrientName} follows one nutrient's optimum across analyses — the series that shows
 * whether a lab's target moved between reports.
 */
interface ReportedOptimumRepository extends EntityRepository<ReportedOptimumId, ReportedOptimum> {

    List<ReportedOptimum> getByLabAnalysisId(LabAnalysisId labAnalysisId);

    /** Batched sibling of {@link #getByLabAnalysisId} — every analysis's optima in one call. */
    List<ReportedOptimum> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds);

    List<ReportedOptimum> getByNutrientName(NutrientName nutrientName);
}
