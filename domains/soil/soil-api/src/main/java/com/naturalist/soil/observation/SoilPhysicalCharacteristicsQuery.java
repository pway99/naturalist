package com.naturalist.soil.observation;

import java.util.Optional;
import java.util.Set;

import com.naturalist.data.EntityQuery;

/**
 * Read port for {@link SoilPhysicalCharacteristics}. Adds the 1:1 {@code forLabAnalysisId} lookup
 * the aggregate factory uses to attach an analysis's physical properties.
 */
public interface SoilPhysicalCharacteristicsQuery
        extends EntityQuery<SoilPhysicalCharacteristicsId, SoilPhysicalCharacteristics,
        SoilPhysicalCharacteristicsCollection> {

    Optional<SoilPhysicalCharacteristics> forLabAnalysisId(LabAnalysisId labAnalysisId);

    /** Batched sibling of {@link #forLabAnalysisId} — the (0-or-1) row per analysis in one call. */
    SoilPhysicalCharacteristicsCollection forLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds);
}
