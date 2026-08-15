package com.naturalist.garden;

import com.naturalist.data.EntityRepository;
import com.naturalist.zone.ZoneName;

import java.util.List;

/**
 * Repository port for {@link Planting}. Package-private (ADR-020); cross-domain access goes
 * through the public {@code PlantingQuery} and {@code GardenPlanQuery}.
 */
interface PlantingRepository extends EntityRepository<PlantingId, Planting> {

    List<Planting> getByCropTypeName(CropTypeName cropTypeName);

    List<Planting> getByZoneName(ZoneName zoneName);
}
