package com.naturalist.soil.observation;

import com.naturalist.data.EntityRepository;

import java.util.List;
import java.util.Set;

/**
 * The single repository for every {@link NutrientReading} across all analyses. Package-private
 * (ADR-020). {@code getByLabAnalysisId} assembles a panel; {@code getByNutrientName} drives the
 * monitoring time series (one nutrient across a profile's analyses over time).
 */
interface NutrientReadingRepository extends EntityRepository<NutrientReadingId, NutrientReading> {

    List<NutrientReading> getByLabAnalysisId(LabAnalysisId labAnalysisId);

    /** Batched sibling of {@link #getByLabAnalysisId} — every analysis's readings in one call. */
    List<NutrientReading> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds);

    List<NutrientReading> getByNutrientName(NutrientName nutrientName);
}
