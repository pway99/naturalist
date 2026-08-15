package com.naturalist.plants.phytochemistry;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantName;

import java.util.List;

@DomainService
class PhytochemicalConstituentEntityRepositoryMock
        extends AbstractTestEntityRepository<PhytochemicalConstituentName, PhytochemicalConstituent, PhytochemicalConstituentTestEntitySource>
        implements PhytochemicalConstituentRepository.PhytochemicalConstituentEntityRepository {

    PhytochemicalConstituentEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PhytochemicalConstituent> getByPlantName(PlantName plantName) {
        observer().arguments("getByPlantName",
                        i -> i.entityName(plantName, "plantName"))
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
