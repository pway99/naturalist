package com.naturalist.garden;

import com.naturalist.data.NaturalistDatabase;

/**
 * Full-graph test wiring for {@code garden-core} tests: constructs the planting query and the
 * aggregate factory, exposing the {@link PlantedZoneQuery} — mirroring the module-level
 * {@code GardenTestContext} but living in core's own test classpath to avoid the Maven cycle
 * (garden-test-context depends on garden-core, so the reverse is impossible).
 */
class GardenTestContextInternal {

    private final PlantedZoneQuery plantedZoneQuery;

    private GardenTestContextInternal(NaturalistDatabase db) {
        PlantingQuery plantingQuery = new PlantingQueryImpl(new PlantingEntityRepositoryMock(db));
        this.plantedZoneQuery = new PlantedZoneQueryImpl(new PlantedZoneFactory(plantingQuery));
    }

    static GardenTestContextInternal create(NaturalistDatabase db) {
        return new GardenTestContextInternal(db);
    }

    PlantedZoneQuery plantedZoneQuery() {
        return plantedZoneQuery;
    }
}
