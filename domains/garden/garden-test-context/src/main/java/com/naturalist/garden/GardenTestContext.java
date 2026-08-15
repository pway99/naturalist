package com.naturalist.garden;

import com.naturalist.data.NaturalistDatabase;

/**
 * Pre-wired, in-memory read surface for the garden bounded context: assembles the entity queries
 * and the {@code GardenPlanFactory}, exposing the {@link GardenPlanQuery} (assembled plan), the
 * {@link CropTypeQuery} (type enumeration) and the {@link PlantingQuery}. Lives in package
 * {@code com.naturalist.garden} for split-package access to garden-core's package-private impls.
 * Goes away when Spring DI replaces the manual composition.
 */
public class GardenTestContext {

    private final CropTypeQuery cropTypeQuery;
    private final PlantingQuery plantingQuery;
    private final GardenPlanQuery gardenPlanQuery;

    private GardenTestContext(NaturalistDatabase db) {
        this.cropTypeQuery = new CropTypeQueryImpl(new CropTypeEntityRepositoryMock(db));
        this.plantingQuery = new PlantingQueryImpl(new PlantingEntityRepositoryMock(db));
        this.gardenPlanQuery =
                new GardenPlanQueryImpl(new GardenPlanFactory(cropTypeQuery, plantingQuery));
    }

    public static GardenTestContext create(NaturalistDatabase db) {
        return new GardenTestContext(db);
    }

    public CropTypeQuery cropTypeQuery() {
        return cropTypeQuery;
    }

    public PlantingQuery plantingQuery() {
        return plantingQuery;
    }

    public GardenPlanQuery gardenPlanQuery() {
        return gardenPlanQuery;
    }
}
