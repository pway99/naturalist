package com.naturalist.soil.observation;

import com.naturalist.data.EntityQuery;
import com.naturalist.soil.SoilProfileName;

/**
 * Read port for {@link LabAnalysisInfo}. Inherits {@code getByName}/{@code findByNameSet}/
 * {@code findPage} from {@link EntityQuery} and adds the profile-scoped reverse lookup the
 * aggregate factory uses to gather a profile's analyses.
 */
public interface LabAnalysisInfoQuery
        extends EntityQuery<LabAnalysisId, LabAnalysisInfo, LabAnalysisInfoCollection> {

    LabAnalysisInfoCollection forSoilProfileName(SoilProfileName soilProfileName);
}
