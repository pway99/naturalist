package com.naturalist.garden;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.zone.ZoneName;

import java.util.Set;

/**
 * Thin adapter for {@link PlantingQuery}: observe, dispatch, delegate (ADR-010).
 */
@DomainService
class PlantingQueryImpl
        extends AbstractEntityQuery<PlantingId, Planting, PlantingCollection, PlantingRepository>
        implements PlantingQuery {

    PlantingQueryImpl(PlantingRepository repository) {
        super(repository);
    }

    @Override
    public PlantingCollection findByNameSet(Set<PlantingId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return PlantingCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public PlantingCollection forCropTypeName(CropTypeName cropTypeName) {
        observer().arguments("forCropTypeName", i -> i.entityName(cropTypeName, "cropTypeName"))
                .throwWhenInvalid();
        return PlantingCollection.of(repository().getByCropTypeName(cropTypeName));
    }

    @Override
    public PlantingCollection forZoneName(ZoneName zoneName) {
        observer().arguments("forZoneName", i -> i.entityName(zoneName, "zoneName"))
                .throwWhenInvalid();
        return PlantingCollection.of(repository().getByZoneName(zoneName));
    }
}
