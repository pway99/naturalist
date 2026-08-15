package com.naturalist.garden;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.zone.ZoneName;

import java.util.List;

/**
 * In-memory {@link PlantingRepository} backed by {@link PlantingTestEntitySource}.
 */
@DomainService
class PlantingEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantingId, Planting, PlantingTestEntitySource>
        implements PlantingRepository {

    protected PlantingEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<Planting> getByCropTypeName(CropTypeName cropTypeName) {
        observer().arguments("getByCropTypeName", i -> i.entityName(cropTypeName, "cropTypeName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(planting -> cropTypeName.equals(planting.cropTypeName()))
                .toList();
    }

    @Override
    public List<Planting> getByZoneName(ZoneName zoneName) {
        observer().arguments("getByZoneName", i -> i.entityName(zoneName, "zoneName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(planting -> zoneName.equals(planting.zoneName()))
                .toList();
    }
}
