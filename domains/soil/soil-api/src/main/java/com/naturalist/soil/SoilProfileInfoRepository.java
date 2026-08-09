package com.naturalist.soil;

import com.naturalist.data.EntityRepository;

/**
 * Repository port for {@link SoilProfileInfo}, the root entity of the {@link SoilProfile}
 * aggregate. Package-private (ADR-020); cross-domain access goes through the public
 * {@code SoilProfileQuery}.
 */
interface SoilProfileInfoRepository extends EntityRepository<SoilProfileName, SoilProfileInfo> {
}
