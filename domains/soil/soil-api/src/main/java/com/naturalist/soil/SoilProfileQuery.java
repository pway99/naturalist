package com.naturalist.soil;

import com.naturalist.soil.observation.LabAnalysisInfo;

import java.util.Optional;

/**
 * Read port for the {@link SoilProfile} aggregate. Unlike the entity queries, the aggregate is
 * not stored — it is assembled on demand by a package-private factory in {@code soil-core} from
 * the persisted {@link SoilProfileInfo} root and its {@link LabAnalysisInfo}
 * children (events are not yet wired and assemble empty). Mirrors the insects
 * {@code TaxonViewQuery} aggregate-read pattern (ADR-010, ADR-020).
 */
public interface SoilProfileQuery {

    /**
     * Assemble the soil profile rooted at the given name, or empty if no such profile exists.
     */
    Optional<SoilProfile> getBySoilProfileName(SoilProfileName soilProfileName);
}
