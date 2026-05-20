package com.naturalist.insects.lifestage;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.LifeStageName;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import com.naturalist.insects.lifestage.InsectLifeStageQuery.LifeStageEntityQuery;

import java.util.Set;

@DomainService
class LifeStageEntityQueryImpl
        extends AbstractEntityQuery<
        LifeStageName,
        LifeStage,
        LifeStageCollection,
        LifeStageRepository.LifeStageEntityRepository>
        implements LifeStageEntityQuery {

    LifeStageEntityQueryImpl(LifeStageRepository.LifeStageEntityRepository repository) {
        super(repository);
    }

    @Override
    public LifeStageCollection findByNameSet(Set<LifeStageName> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return LifeStageCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public LifeStageCollection forParentName(InsectRankName parentName) {
        observer().arguments("forParentName", i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        return LifeStageCollection.of(repository().getByParentName(parentName));
    }
}
