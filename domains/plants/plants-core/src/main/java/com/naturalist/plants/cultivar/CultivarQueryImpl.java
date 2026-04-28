package com.naturalist.plants.cultivar;

import com.naturalist.observability.Observer;

class CultivarQueryImpl implements CultivarQuery {

    private final CultivarEntityQuery cultivarEntityQuery;

    CultivarQueryImpl(CultivarEntityQuery cultivarEntityQuery) {
        Observer.forClass(CultivarQueryImpl.class).arguments("constructor", i -> i
                        .notNull(cultivarEntityQuery, "cultivarEntityQuery"))
                .throwWhenInvalid();
        this.cultivarEntityQuery = cultivarEntityQuery;
    }

    @Override
    public CultivarEntityQuery cultivars() {
        return cultivarEntityQuery;
    }
}
