package com.naturalist.garden;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;

import java.util.Optional;

/**
 * Thin adapter for {@link PlantedZoneQuery}: observe, dispatch, delegate to the aggregate factory
 * (ADR-010).
 * <p>
 * Factory-backed query bean: {@link PlantedZoneFactory} is itself {@code @DomainService}, so it is
 * injected here like any other collaborator.
 */
@DomainService
class PlantedZoneQueryImpl implements PlantedZoneQuery {

    private final Observer observer = Observer.forClass(getClass());
    private final PlantedZoneFactory factory;

    PlantedZoneQueryImpl(PlantedZoneFactory factory) {
        observer.arguments("constructor", i -> i.notNull(factory, "factory")).throwWhenInvalid();
        this.factory = factory;
    }

    @Override
    public Optional<PlantedZone> getByZoneName(ZoneName zoneName) {
        observer.arguments("getByZoneName", i -> i.entityName(zoneName, "zoneName"))
                .throwWhenInvalid();
        return factory.buildByZoneName(zoneName);
    }

    @Override
    public Optional<PlantedZone> getBySubZoneName(ZoneName zoneName, SubZoneName subZoneName) {
        observer.arguments("getBySubZoneName", i -> i
                        .entityName(zoneName, "zoneName")
                        .entityName(subZoneName, "subZoneName"))
                .throwWhenInvalid();
        return factory.buildBySubZoneName(zoneName, subZoneName);
    }
}
