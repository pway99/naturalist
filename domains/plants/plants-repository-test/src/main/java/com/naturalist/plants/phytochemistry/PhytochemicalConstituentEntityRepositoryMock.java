package com.naturalist.plants.phytochemistry;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantName;

import java.util.List;

@DomainService
public class PhytochemicalConstituentEntityRepositoryMock
        extends AbstractTestEntityRepository<PhytochemicalConstituentName, PhytochemicalConstituent, PhytochemicalConstituentTestEntitySource>
        implements PhytochemicalConstituentRepository.PhytochemicalConstituentEntityRepository {

    protected PhytochemicalConstituentEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PhytochemicalConstituent> getByPlantName(PlantName plantName) {
        return testEntitySource().entityStream()
                .filter(c -> c.plantName().equals(plantName))
                .toList();
    }

    @Override
    public List<PhytochemicalConstituent> getByCompoundName(CompoundName compoundName) {
        return testEntitySource().entityStream()
                .filter(c -> c.compoundName().equals(compoundName))
                .toList();
    }
}
