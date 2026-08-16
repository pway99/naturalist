package com.naturalist.garden;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantRankName;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;

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
    public List<Planting> getByZoneName(ZoneName zoneName) {
        observer().arguments("getByZoneName", i -> i.entityName(zoneName, "zoneName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(planting -> zoneName.equals(planting.zoneName()))
                .toList();
    }

    @Override
    public List<Planting> getBySubZoneName(SubZoneName subZoneName) {
        observer().arguments("getBySubZoneName", i -> i.entityName(subZoneName, "subZoneName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(planting -> subZoneName.equals(planting.subZoneName()))
                .toList();
    }

    @Override
    public List<Planting> getBySubject(PlantRankName subject) {
        observer().arguments("getBySubject", i -> i.identifier(subject, "subject"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(planting -> subject.equals(planting.subject()))
                .toList();
    }
}
