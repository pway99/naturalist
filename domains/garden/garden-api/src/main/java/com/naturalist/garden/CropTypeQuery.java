package com.naturalist.garden;

import com.naturalist.data.EntityQuery;

/**
 * Read port for {@link CropType} — the aggregate root's identity. Inherits {@code getByName},
 * {@code findByNameSet} and {@code findPage} from {@link EntityQuery}; the crop carries no
 * reverse lookup of its own because nothing points at a crop except by name.
 */
public interface CropTypeQuery extends EntityQuery<CropTypeName, CropType, CropTypeCollection> {
}
