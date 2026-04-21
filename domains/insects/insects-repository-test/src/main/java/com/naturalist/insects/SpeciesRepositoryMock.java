package com.naturalist.insects;

import com.naturalist.data.AbstractTestNamedEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;

class SpeciesRepositoryMock
        extends AbstractTestNamedEntityRepository<InsectSpeciesName, InsectSpecies, InsectSpeciesTestEntitySource>
        implements InsectRepository.SpeciesRepository {

    SpeciesRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectSpeciesName> getAllSpeciesNames() {
        return testEntitySource().entityStream()
                .map(InsectSpecies::name)
                .toList();
    }
}
