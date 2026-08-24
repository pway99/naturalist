package com.naturalist.soil.observation;

import com.naturalist.data.EntityQuery;

import java.util.Set;

/**
 * Read port for {@link ReportedOptimum}. Mirrors {@link NutrientReadingQuery}'s two reverse
 * lookups, because the two live at the same grain: by analysis ({@code forLabAnalysisId} — the
 * optima printed beside a panel) and by nutrient ({@code forNutrientName} — one nutrient's target
 * across analyses over time).
 */
public interface ReportedOptimumQuery
        extends EntityQuery<ReportedOptimumId, ReportedOptimum, ReportedOptimumCollection> {

    ReportedOptimumCollection forLabAnalysisId(LabAnalysisId labAnalysisId);

    /** Batched sibling of {@link #forLabAnalysisId} — every analysis's optima in one call. */
    ReportedOptimumCollection forLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds);

    ReportedOptimumCollection forNutrientName(NutrientName nutrientName);
}
