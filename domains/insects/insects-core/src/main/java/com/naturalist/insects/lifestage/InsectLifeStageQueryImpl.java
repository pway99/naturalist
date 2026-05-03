package com.naturalist.insects.lifestage;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

@DomainService
class InsectLifeStageQueryImpl implements InsectLifeStageQuery {

    private final LifeStageEntityQuery lifeStageEntityQuery;

    InsectLifeStageQueryImpl(LifeStageEntityQuery lifeStageEntityQuery) {
        Observer.forClass(InsectLifeStageQueryImpl.class).arguments("constructor", i -> i
                        .notNull(lifeStageEntityQuery, "lifeStageEntityQuery"))
                .throwWhenInvalid();
        this.lifeStageEntityQuery = lifeStageEntityQuery;
    }

    @Override
    public LifeStageEntityQuery lifeStages() {
        return lifeStageEntityQuery;
    }
}