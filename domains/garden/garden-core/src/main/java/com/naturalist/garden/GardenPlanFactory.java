package com.naturalist.garden;

import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;

import java.util.Optional;

/**
 * Place-keyed assembly of the {@link GardenPlan} read model from the {@link Planting}s of a bed,
 * at either grain: a whole zone, or one of its subdivisions.
 * <p>
 * Package-private concrete factory (no interface, no {@code Impl} suffix) per ADR-020, mirroring
 * {@code SoilProfileFactory}. Per the producer/consumer rule (ADR-017) it validates its own
 * arguments with {@code throwWhenInvalid()} but only observes the assembled plan.
 */
class GardenPlanFactory {

    private final Observer observer = Observer.forClass(getClass());
    private final PlantingQuery plantingQuery;

    GardenPlanFactory(PlantingQuery plantingQuery) {
        observer.arguments("constructor", i -> i.notNull(plantingQuery, "plantingQuery"))
                .throwWhenInvalid();
        this.plantingQuery = plantingQuery;
    }

    Optional<GardenPlan> buildByZoneName(ZoneName zoneName) {
        observer.arguments("buildByZoneName", i -> i.entityName(zoneName, "zoneName"))
                .throwWhenInvalid();
        return plan(zoneName, null, plantingQuery.forZoneName(zoneName));
    }

    Optional<GardenPlan> buildBySubZoneName(ZoneName zoneName, SubZoneName subZoneName) {
        observer.arguments("buildBySubZoneName", i -> i
                        .entityName(zoneName, "zoneName")
                        .entityName(subZoneName, "subZoneName"))
                .throwWhenInvalid();
        return plan(zoneName, subZoneName, plantingQuery.forSubZoneName(subZoneName));
    }

    /** No plantings means garden has nothing to say about that place — not an empty plan. */
    private Optional<GardenPlan> plan(ZoneName zoneName, SubZoneName subZoneName,
                                      PlantingCollection plantings) {
        if (plantings.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(observe(new GardenPlan(zoneName, subZoneName, plantings)));
    }

    private GardenPlan observe(GardenPlan gardenPlan) {
        observer.observable(gardenPlan, "gardenPlan").observe(Level.WARN);
        return gardenPlan;
    }
}
