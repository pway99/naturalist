package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;
import java.util.Set;

class InsectSpeciesRepositoryMock
        extends AbstractTestEntityRepository<InsectSpeciesName, InsectSpecies, InsectSpeciesTestEntitySource>
        implements InsectRepository.SpeciesRepository {

    InsectSpeciesRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectSpecies> getByGenusName(InsectGenusName genusName) {
        observer().arguments("getByGenusName",
                        i -> i.entityName(genusName, "genusName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(s -> genusName.equals(s.genusName()))
                .toList();
    }

    @Override
    public List<InsectSpecies> getByGenusNames(Set<InsectGenusName> genusNames) {
        observer().arguments("getByGenusNames",
                        i -> i.entityNameCollection(genusNames, "genusNames"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(s -> genusNames.contains(s.genusName()))
                .toList();
    }
}
