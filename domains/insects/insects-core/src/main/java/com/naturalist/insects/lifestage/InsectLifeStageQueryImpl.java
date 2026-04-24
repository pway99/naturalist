package com.naturalist.insects.lifestage;

import com.naturalist.observability.Observer;

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