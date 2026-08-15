package com.naturalist.garden;

import com.naturalist.data.NaturalistDatabase;

/**
 * Full-graph test wiring for {@code garden-core} tests: constructs the planting query and the
 * aggregate factory, exposing the {@link GardenPlanQuery} — mirroring the module-level
 * {@code GardenTestContext} but living in core's own test classpath to avoid the Maven cycle
 * (garden-test-context depends on garden-core, so the reverse is impossible).
 */
class GardenTestContextInternal {

    private final GardenPlanQuery gardenPlanQuery;

    private GardenTestContextInternal(NaturalistDatabase db) {
        PlantingQuery plantingQuery = new PlantingQueryImpl(new PlantingEntityRepositoryMock(db));
        this.gardenPlanQuery = new GardenPlanQueryImpl(new GardenPlanFactory(plantingQuery));
    }

    static GardenTestContextInternal create(NaturalistDatabase db) {
        return new GardenTestContextInternal(db);
    }

    GardenPlanQuery gardenPlanQuery() {
        return gardenPlanQuery;
    }
}
