package com.naturalist.garden;

import com.naturalist.test.query.nofanout.SelectTrace;
import com.naturalist.data.NaturalistDatabase;

/**
 * Pre-wired, in-memory read surface for the garden bounded context: assembles the planting query
 * and the {@code PlantedZoneFactory}, exposing the {@link PlantedZoneQuery} (a bed and what is in it)
 * and the {@link PlantingQuery}. Lives in package {@code com.naturalist.garden} for split-package
 * access to garden-core's package-private impls. Goes away when Spring DI replaces the manual
 * composition.
 */
public class GardenTestContext {

    private final PlantingQuery plantingQuery;
    private final PlantedZoneQuery plantedZoneQuery;

    private GardenTestContext(NaturalistDatabase db) {
        this.plantingQuery = new PlantingQueryImpl(new PlantingEntityRepositoryMock(db));
        this.plantedZoneQuery = new PlantedZoneQueryImpl(new PlantedZoneFactory(plantingQuery));
    }

    public static GardenTestContext create(NaturalistDatabase db) {
        return new GardenTestContext(db);
    }

    public PlantingQuery plantingQuery() {
        return SelectTrace.traced(PlantingQuery.class, plantingQuery);
    }

    public PlantedZoneQuery plantedZoneQuery() {
        return SelectTrace.traced(PlantedZoneQuery.class, plantedZoneQuery);
    }
}
