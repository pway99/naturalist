package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;

@MockDomainService
class PlantFamilyRepositoryMock
        extends AbstractTestEntityRepository<PlantFamilyName, PlantFamily, PlantFamilyTestEntitySource>
        implements PlantRepository.FamilyRepository {

    PlantFamilyRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PlantFamily> getByOrderName(PlantOrderName orderName) {
        MethodObserver getByOrderName = observer().forMethod("getByOrderName");
        getByOrderName.arguments(i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(f -> orderName.equals(f.orderName()))
                .toList();
        getByOrderName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
