package com.naturalist.soil.observation;

import java.util.Optional;

import com.naturalist.data.EntityQuery;

/**
 * Read port for {@link SoilPhysicalCharacteristics}. Adds the 1:1 {@code forLabAnalysisId} lookup
 * the aggregate factory uses to attach an analysis's physical properties.
 */
public interface SoilPhysicalCharacteristicsQuery
        extends EntityQuery<SoilPhysicalCharacteristicsId, SoilPhysicalCharacteristics,
        SoilPhysicalCharacteristicsCollection> {

    Optional<SoilPhysicalCharacteristics> forLabAnalysisId(LabAnalysisId labAnalysisId);
}
