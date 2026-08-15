package com.naturalist.garden;

import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.Optional;

/**
 * Name-keyed assembly of the {@link GardenPlan} read model from its persisted parts: the
 * {@link CropType} root and the {@link Planting}s of that type.
 * <p>
 * Package-private concrete factory (no interface, no {@code Impl} suffix) per ADR-020, mirroring
 * {@code SoilProfileFactory}. Per the producer/consumer rule (ADR-017) it validates its own
 * arguments with {@code throwWhenInvalid()} but only observes the assembled plan.
 */
class GardenPlanFactory {

    private final Observer observer = Observer.forClass(getClass());
    private final CropTypeQuery cropTypeQuery;
    private final PlantingQuery plantingQuery;

    GardenPlanFactory(CropTypeQuery cropTypeQuery, PlantingQuery plantingQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(cropTypeQuery, "cropTypeQuery")
                        .notNull(plantingQuery, "plantingQuery"))
                .throwWhenInvalid();
        this.cropTypeQuery = cropTypeQuery;
        this.plantingQuery = plantingQuery;
    }

    Optional<GardenPlan> buildByName(CropTypeName cropTypeName) {
        observer.arguments("buildByName", i -> i.entityName(cropTypeName, "cropTypeName"))
                .throwWhenInvalid();
        return cropTypeQuery.getByName(cropTypeName)
                .map(cropType -> observe(
                        new GardenPlan(cropType, plantingQuery.forCropTypeName(cropTypeName))));
    }

    private GardenPlan observe(GardenPlan gardenPlan) {
        observer.observable(gardenPlan, "gardenPlan").observe(Level.WARN);
        return gardenPlan;
    }
}
