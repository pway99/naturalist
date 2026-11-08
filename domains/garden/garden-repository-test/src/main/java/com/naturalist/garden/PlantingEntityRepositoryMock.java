package com.naturalist.garden;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.plants.PlantRankName;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;

import java.util.List;

/**
 * In-memory {@link PlantingRepository} backed by {@link PlantingTestEntitySource}.
 */
@MockDomainService
class PlantingEntityRepositoryMock
        extends AbstractTestEntityRepository<PlantingId, Planting, PlantingTestEntitySource>
        implements PlantingRepository {

    protected PlantingEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<Planting> getByZoneName(ZoneName zoneName) {
        MethodObserver getByZoneName = observer().forMethod("getByZoneName");
        getByZoneName.arguments(i -> i.entityName(zoneName, "zoneName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(planting -> zoneName.equals(planting.zoneName()))
                .toList();
        getByZoneName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<Planting> getBySubZoneName(SubZoneName subZoneName) {
        MethodObserver getBySubZoneName = observer().forMethod("getBySubZoneName");
        getBySubZoneName.arguments(i -> i.entityName(subZoneName, "subZoneName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(planting -> subZoneName.equals(planting.subZoneName()))
                .toList();
        getBySubZoneName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<Planting> getByPlantName(PlantRankName plantName) {
        MethodObserver getByPlantName = observer().forMethod("getByPlantName");
        getByPlantName.arguments(i -> i.identifier(plantName, "plantName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(planting -> plantName.equals(planting.plantName()))
                .toList();
        getByPlantName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
