package com.naturalist.plants.phytochemistry;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.plants.PlantRankName;

import java.util.List;

@MockDomainService
class PlantPhytochemicalConstituentRepositoryMock
        extends AbstractTestEntityRepository<PhytochemicalConstituentName, PhytochemicalConstituent, PlantPhytochemicalConstituentTestEntitySource>
        implements PhytochemicalConstituentRepository {

    PlantPhytochemicalConstituentRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PhytochemicalConstituent> getByPlantName(PlantRankName plantName) {
        MethodObserver getByPlantName = observer().forMethod("getByPlantName");
        getByPlantName.arguments(i -> i.identifier(plantName, "plantName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(c -> plantName.equals(c.plantName()))
                .toList();
        getByPlantName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<PhytochemicalConstituent> getByCompoundName(CompoundName compoundName) {
        MethodObserver getByCompoundName = observer().forMethod("getByCompoundName");
        getByCompoundName.arguments(i -> i.entityName(compoundName, "compoundName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(c -> compoundName.equals(c.compoundName()))
                .toList();
        getByCompoundName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
