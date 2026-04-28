package com.naturalist.plants.heritage;

import com.naturalist.observability.Observer;

class SeedLineageQueryImpl implements SeedLineageQuery {

    private final SeedLineageEntityQuery seedLineageEntityQuery;

    SeedLineageQueryImpl(SeedLineageEntityQuery seedLineageEntityQuery) {
        Observer.forClass(SeedLineageQueryImpl.class).arguments("constructor", i -> i
                        .notNull(seedLineageEntityQuery, "seedLineageEntityQuery"))
                .throwWhenInvalid();
        this.seedLineageEntityQuery = seedLineageEntityQuery;
    }

    @Override
    public SeedLineageEntityQuery lineages() {
        return seedLineageEntityQuery;
    }
}
