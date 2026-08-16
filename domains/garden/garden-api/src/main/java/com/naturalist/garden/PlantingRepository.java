package com.naturalist.garden;

import com.naturalist.data.EntityRepository;
import com.naturalist.plants.PlantRankName;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;

import java.util.List;

/**
 * Repository port for {@link Planting}. Package-private (ADR-020); cross-domain access goes
 * through the public {@code PlantingQuery} and {@code PlantedZoneQuery}.
 */
interface PlantingRepository extends EntityRepository<PlantingId, Planting> {

    List<Planting> getByZoneName(ZoneName zoneName);

    List<Planting> getBySubZoneName(SubZoneName subZoneName);

    List<Planting> getByPlantName(PlantRankName plantName);
}
