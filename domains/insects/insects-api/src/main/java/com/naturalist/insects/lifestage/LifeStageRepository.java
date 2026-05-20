package com.naturalist.insects.lifestage;

import com.naturalist.Incubating;
import com.naturalist.data.EntityRepository;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.LifeStageName;

import java.util.List;

@Incubating("Investigating a pattern where EntityRepositories are nested within a single interface")
interface LifeStageRepository {
    interface LifeStageEntityRepository extends EntityRepository<LifeStageName, LifeStage> {

        List<LifeStage> getByParentName(InsectRankName parentName);
    }
}
