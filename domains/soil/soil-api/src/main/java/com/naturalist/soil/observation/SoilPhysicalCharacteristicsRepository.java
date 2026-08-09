package com.naturalist.soil.observation;

import com.naturalist.data.EntityRepository;

import java.util.Optional;

/**
 * Repository for {@link SoilPhysicalCharacteristics} — one row per analysis. Package-private
 * (ADR-020). {@code getByLabAnalysisId} fetches an analysis's physical properties for assembly and
 * for monitoring a physical property across analyses over time.
 */
interface SoilPhysicalCharacteristicsRepository
        extends EntityRepository<SoilPhysicalCharacteristicsId, SoilPhysicalCharacteristics> {

    Optional<SoilPhysicalCharacteristics> getByLabAnalysisId(LabAnalysisId labAnalysisId);
}
