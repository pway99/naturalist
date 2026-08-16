package com.naturalist.garden;

import com.naturalist.data.EntityQuery;
import com.naturalist.plants.PlantRankName;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;

/**
 * Read port for {@link Planting}. Two reverse lookups, matching the two questions asked of a
 * planting: what is in this bed ({@code forZoneName}, or {@code forSubZoneName} for one
 * subdivision of it — a planted zone at either grain), and where have we grown this
 * ({@code forPlantName} — one taxon across beds and seasons).
 */
public interface PlantingQuery extends EntityQuery<PlantingId, Planting, PlantingCollection> {

    PlantingCollection forZoneName(ZoneName zoneName);

    PlantingCollection forSubZoneName(SubZoneName subZoneName);

    /**
     * Plantings whose plantName is exactly this rank name. Matching is exact, not
     * hierarchical: querying the genus {@code salvia} returns plantings recorded at that
     * genus, not plantings of {@code salvia-officinalis}. A hierarchy-walking variant
     * would be a separate method, as {@code ImageQuery.forRankHierarchy} is on the insect
     * side.
     */
    PlantingCollection forPlantName(PlantRankName plantName);
}
