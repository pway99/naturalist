package com.naturalist.plants.phytochemistry;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
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
        observer().arguments("getByPlantName",
                        i -> i.identifier(plantName, "plantName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(c -> plantName.equals(c.plantName()))
                .toList();
    }

    @Override
    public List<PhytochemicalConstituent> getByCompoundName(CompoundName compoundName) {
        observer().arguments("getByCompoundName",
                        i -> i.entityName(compoundName, "compoundName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(c -> compoundName.equals(c.compoundName()))
                .toList();
    }
}
