package com.naturalist.insects.lifestage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.LifeStageName;

import java.util.List;

public class InsectLifeStageEntityRepositoryMock
        extends AbstractTestEntityRepository<LifeStageName, LifeStage, InsectLifeStageTestEntitySource>
        implements LifeStageRepository.LifeStageEntityRepository {

    protected InsectLifeStageEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<LifeStage> getByParentName(InsectRankName parentName) {
        observer().arguments("getByParentName",
                        i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(stage -> parentName.equals(stage.parentName()))
                .toList();
    }
}
