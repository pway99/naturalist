package com.naturalist.insects.lifestage;

import com.naturalist.data.EntityRepository;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.LifeStageName;

import java.util.List;

interface LifeStageRepository {
    interface LifeStageEntityRepository extends EntityRepository<LifeStageName, LifeStage> {

        List<LifeStage> getByParentName(InsectRankName parentName);
    }
}
