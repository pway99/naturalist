package com.naturalist.garden;

import com.naturalist.data.EntityQuery;
import com.naturalist.zone.ZoneName;

/**
 * Read port for {@link Planting}. Two reverse lookups, matching the two questions asked of a
 * planting: what did we grow of this crop type ({@code forCropTypeName}), and what is in this bed
 * ({@code forZoneName}).
 */
public interface PlantingQuery extends EntityQuery<PlantingId, Planting, PlantingCollection> {

    PlantingCollection forCropTypeName(CropTypeName cropTypeName);

    PlantingCollection forZoneName(ZoneName zoneName);
}
