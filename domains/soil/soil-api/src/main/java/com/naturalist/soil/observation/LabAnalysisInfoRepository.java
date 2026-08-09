package com.naturalist.soil.observation;

import com.naturalist.data.EntityRepository;
import com.naturalist.soil.SoilProfileName;

import java.util.List;

/**
 * Repository port for {@link LabAnalysisInfo}, a child entity of the {@link com.naturalist.soil.SoilProfile}
 * aggregate. Package-private (ADR-020). The {@code getBySoilProfileName} reverse lookup lets the
 * aggregate factory gather a profile's analyses without a cross-domain join.
 */
interface LabAnalysisInfoRepository extends EntityRepository<LabAnalysisId, LabAnalysisInfo> {

    /**
     * The analyses on record for a soil profile, joined on the typed
     * {@link LabAnalysisInfo#soilProfileName()} back-reference.
     */
    List<LabAnalysisInfo> getBySoilProfileName(SoilProfileName soilProfileName);
}
