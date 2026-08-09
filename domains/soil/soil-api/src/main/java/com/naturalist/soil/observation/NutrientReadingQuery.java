package com.naturalist.soil.observation;

import com.naturalist.data.EntityQuery;

/**
 * Read port for {@link NutrientReading}. Adds the two reverse lookups the domain runs on: by
 * analysis ({@code forLabAnalysisId} — assemble a panel) and by nutrient ({@code forNutrientName} —
 * the monitoring time series, one nutrient across a profile's analyses).
 */
public interface NutrientReadingQuery
        extends EntityQuery<NutrientReadingId, NutrientReading, NutrientReadingCollection> {

    NutrientReadingCollection forLabAnalysisId(LabAnalysisId labAnalysisId);

    NutrientReadingCollection forNutrientName(NutrientName nutrientName);
}
