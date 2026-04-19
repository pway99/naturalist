package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Observer;

class SpeciesRepositoryMock extends AbstractTestEntityRepository<InsectSpeciesId, InsectSpeciesName, InsectSpecies, InsectSpeciesTestEntitySource>
        implements InsectRepository.SpeciesRepository {
    private static final Observer observer = Observer.forClass(SpeciesRepositoryMock.class);

    protected SpeciesRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Observer observer() {
        return observer;
    }
}
