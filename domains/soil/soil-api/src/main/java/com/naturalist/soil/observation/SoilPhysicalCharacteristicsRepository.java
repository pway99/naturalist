package com.naturalist.soil.observation;

import com.naturalist.data.EntityRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for {@link SoilPhysicalCharacteristics} — one row per analysis. Package-private
 * (ADR-020). {@code getByLabAnalysisId} fetches an analysis's physical properties for assembly and
 * for monitoring a physical property across analyses over time.
 */
interface SoilPhysicalCharacteristicsRepository
        extends EntityRepository<SoilPhysicalCharacteristicsId, SoilPhysicalCharacteristics> {

    Optional<SoilPhysicalCharacteristics> getByLabAnalysisId(LabAnalysisId labAnalysisId);

    /** Batched sibling of {@link #getByLabAnalysisId} — the (0-or-1) row per analysis in one call. */
    List<SoilPhysicalCharacteristics> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds);
}
