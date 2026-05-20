package com.naturalist.insects.lifestage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.LifeStageName;

import java.util.List;

@DomainService
public class LifeStageEntityRepositoryMock
        extends AbstractTestEntityRepository<LifeStageName, LifeStage, LifeStageTestEntitySource>
        implements LifeStageRepository.LifeStageEntityRepository {

    protected LifeStageEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<LifeStage> getByParentName(InsectRankName parentName) {
        return testEntitySource().entityStream()
                .filter(stage -> stage.name().parentSlug().equals(parentName.value()))
                .toList();
    }
}
