package com.naturalist.garden;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;

import java.util.Optional;

/**
 * Place-keyed assembly of the {@link PlantedZone} read model from the {@link Planting}s of a bed,
 * at either grain: a whole zone, or one of its subdivisions.
 * <p>
 * Package-private concrete factory (no interface, no {@code Impl} suffix) per ADR-020, mirroring
 * {@code SoilProfileFactory}. Per the producer/consumer rule (ADR-017) it validates its own
 * arguments with {@code throwWhenInvalid()} but only observes the assembled record.
 */
@DomainService
class PlantedZoneFactory {

    private final Observer observer = Observer.forClass(getClass());
    private final PlantingQuery plantingQuery;

    PlantedZoneFactory(PlantingQuery plantingQuery) {
        observer.arguments("constructor", i -> i.notNull(plantingQuery, "plantingQuery"))
                .throwWhenInvalid();
        this.plantingQuery = plantingQuery;
    }

    Optional<PlantedZone> buildByZoneName(ZoneName zoneName) {
        observer.arguments("buildByZoneName", i -> i.entityName(zoneName, "zoneName"))
                .throwWhenInvalid();
        return assemble(zoneName, null, plantingQuery.forZoneName(zoneName));
    }

    Optional<PlantedZone> buildBySubZoneName(ZoneName zoneName, SubZoneName subZoneName) {
        observer.arguments("buildBySubZoneName", i -> i
                        .entityName(zoneName, "zoneName")
                        .entityName(subZoneName, "subZoneName"))
                .throwWhenInvalid();
        return assemble(zoneName, subZoneName, plantingQuery.forSubZoneName(subZoneName));
    }

    /** No plantings means garden has nothing to say about that place — not an empty record. */
    private Optional<PlantedZone> assemble(ZoneName zoneName, SubZoneName subZoneName,
                                           PlantingCollection plantings) {
        if (plantings.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(observe(new PlantedZone(zoneName, subZoneName, plantings)));
    }

    private PlantedZone observe(PlantedZone plantedZone) {
        observer.observable(plantedZone, "plantedZone").observe(Level.WARN);
        return plantedZone;
    }
}
