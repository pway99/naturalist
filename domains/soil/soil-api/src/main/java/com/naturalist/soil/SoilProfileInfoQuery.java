package com.naturalist.soil;

import com.naturalist.data.EntityQuery;

/**
 * Read port for {@link SoilProfileInfo} — the aggregate root's identity and spatial anchor.
 * Inherits {@code getByName}, {@code findByNameSet}, {@code findPage} from {@link EntityQuery}.
 */
public interface SoilProfileInfoQuery
        extends EntityQuery<SoilProfileName, SoilProfileInfo, SoilProfileInfoCollection> {
}
