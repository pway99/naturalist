package com.naturalist.garden;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantName;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;

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
    public PlantingCollection forZoneName(ZoneName zoneName) {
        observer().arguments("forZoneName", i -> i.entityName(zoneName, "zoneName"))
                .throwWhenInvalid();
        return PlantingCollection.of(repository().getByZoneName(zoneName));
    }

    @Override
    public PlantingCollection forSubZoneName(SubZoneName subZoneName) {
        observer().arguments("forSubZoneName", i -> i.entityName(subZoneName, "subZoneName"))
                .throwWhenInvalid();
        return PlantingCollection.of(repository().getBySubZoneName(subZoneName));
    }

    @Override
    public PlantingCollection forPlantName(PlantName plantName) {
        observer().arguments("forPlantName", i -> i.entityName(plantName, "plantName"))
                .throwWhenInvalid();
        return PlantingCollection.of(repository().getByPlantName(plantName));
    }
}
