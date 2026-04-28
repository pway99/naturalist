package com.naturalist.plants.phytochemistry;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

public class PhytochemicalConstituentEntityRepositoryMock
        extends AbstractTestEntityRepository<PhytochemicalConstituentName, PhytochemicalConstituent, PhytochemicalConstituentTestEntitySource>
        implements PhytochemicalConstituentRepository.PhytochemicalConstituentEntityRepository {

    protected PhytochemicalConstituentEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
