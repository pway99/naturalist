package com.naturalist.soil.observation;

import com.naturalist.data.EntityQuery;

import java.util.Set;

/**
 * Read port for {@link NutrientReading}. Adds the two reverse lookups the domain runs on: by
 * analysis ({@code forLabAnalysisId} — assemble a panel) and by nutrient ({@code forNutrientName} —
 * the monitoring time series, one nutrient across a profile's analyses).
 */
public interface NutrientReadingQuery
        extends EntityQuery<NutrientReadingId, NutrientReading, NutrientReadingCollection> {

    NutrientReadingCollection forLabAnalysisId(LabAnalysisId labAnalysisId);

    /** Batched sibling of {@link #forLabAnalysisId} — every analysis's readings in one call. */
    NutrientReadingCollection forLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds);

    NutrientReadingCollection forNutrientName(NutrientName nutrientName);
}
