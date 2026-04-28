package com.naturalist.plants.cultivar;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

public class CultivarEntityRepositoryMock
        extends AbstractTestEntityRepository<CultivarName, Cultivar, CultivarTestEntitySource>
        implements CultivarRepository.CultivarEntityRepository {

    protected CultivarEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
