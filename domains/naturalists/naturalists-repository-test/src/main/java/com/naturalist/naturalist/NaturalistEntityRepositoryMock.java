package com.naturalist.naturalist;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

class NaturalistEntityRepositoryMock
        extends AbstractTestEntityRepository<NaturalistName, Naturalist, NaturalistTestEntitySource>
        implements NaturalistRepository.NaturalistEntityRepository {

    NaturalistEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
