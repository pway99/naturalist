package com.naturalist.plants.phytochemistry;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentEntityCollections.PhytochemicalConstituentCollection;

import java.util.Set;

@DomainService
class PhytochemicalConstituentEntityQueryImpl
        extends AbstractEntityQuery<PhytochemicalConstituentName, PhytochemicalConstituent, PhytochemicalConstituentCollection, PhytochemicalConstituentRepository.PhytochemicalConstituentEntityRepository>
        implements PhytochemicalConstituentQuery.PhytochemicalConstituentEntityQuery {

    PhytochemicalConstituentEntityQueryImpl(PhytochemicalConstituentRepository.PhytochemicalConstituentEntityRepository repository) {
        super(repository);
    }

    @Override
    public PhytochemicalConstituentCollection findByNameSet(Set<PhytochemicalConstituentName> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();
        return PhytochemicalConstituentCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public PhytochemicalConstituentCollection forPlantName(PlantName plantName) {
        observer().arguments("forPlantName", i -> i
                        .entityName(plantName, "plantName"))
                .throwWhenInvalid();
        return PhytochemicalConstituentCollection.of(repository().getByPlantName(plantName));
    }

    @Override
    public PhytochemicalConstituentCollection forCompoundName(CompoundName compoundName) {
        observer().arguments("forCompoundName", i -> i
                        .entityName(compoundName, "compoundName"))
                .throwWhenInvalid();
        return PhytochemicalConstituentCollection.of(repository().getByCompoundName(compoundName));
    }
}
