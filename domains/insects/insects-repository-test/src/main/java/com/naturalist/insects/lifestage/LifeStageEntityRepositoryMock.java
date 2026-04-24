package com.naturalist.insects.lifestage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.LifeStageName;

import java.util.List;

public class LifeStageEntityRepositoryMock
        extends AbstractTestEntityRepository<LifeStageName, LifeStage, LifeStageTestEntitySource>
        implements LifeStageRepository.LifeStageEntityRepository {

    protected LifeStageEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<LifeStage> getBySpeciesName(InsectSpeciesName speciesName) {
        return testEntitySource().entityStream()
                .filter(stage -> stage.name().speciesName().equals(speciesName))
                .toList();
    }
}
