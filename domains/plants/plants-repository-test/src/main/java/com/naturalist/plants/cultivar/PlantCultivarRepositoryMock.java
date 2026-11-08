package com.naturalist.plants.cultivar;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.plants.PlantSpeciesName;

import java.util.List;

@MockDomainService
class PlantCultivarRepositoryMock
        extends AbstractTestEntityRepository<CultivarName, Cultivar, PlantCultivarTestEntitySource>
        implements CultivarRepository {

    PlantCultivarRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<Cultivar> getByPlantName(PlantSpeciesName plantName) {
        MethodObserver getByPlantName = observer().forMethod("getByPlantName");
        getByPlantName.arguments(i -> i.entityName(plantName, "plantName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(c -> plantName.equals(c.plantName()))
                .toList();
        getByPlantName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
