package com.naturalist.insects.lifestage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.insects.LifeStageName;

public class LifeStageEntityRepositoryMock
        extends AbstractTestEntityRepository<LifeStageName, LifeStage, LifeStageTestEntitySource>
        implements LifeStageRepository.LifeStageEntityRepository {

    protected LifeStageEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
