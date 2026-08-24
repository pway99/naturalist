package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;
import java.util.Set;

class PlantSpeciesRepositoryMock
        extends AbstractTestEntityRepository<PlantSpeciesName, PlantSpecies, PlantSpeciesTestEntitySource>
        implements PlantRepository.SpeciesRepository {

    PlantSpeciesRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PlantSpecies> getByGenusName(PlantGenusName genusName) {
        observer().arguments("getByGenusName", i -> i.entityName(genusName, "genusName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(s -> genusName.equals(s.genusName()))
                .toList();
    }

    @Override
    public List<PlantSpecies> getByGenusNames(Set<PlantGenusName> genusNames) {
        observer().arguments("getByGenusNames", i -> i.entityNameCollection(genusNames, "genusNames"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(s -> genusNames.contains(s.genusName()))
                .toList();
    }
}
