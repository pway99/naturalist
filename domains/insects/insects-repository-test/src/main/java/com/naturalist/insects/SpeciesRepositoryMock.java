package com.naturalist.insects;

import com.naturalist.data.AbstractTestNamedEntityRepository;
import com.naturalist.data.NaturalistDatabase;

class SpeciesRepositoryMock
        extends AbstractTestNamedEntityRepository<InsectSpeciesName, InsectSpecies, InsectSpeciesTestEntitySource>
        implements InsectRepository.SpeciesRepository {

    SpeciesRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
