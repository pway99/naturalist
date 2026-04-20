package com.naturalist.insects;

import com.naturalist.observability.Observer;

class InsectQueryImpl implements InsectQuery {

    private final SpeciesQuery speciesQuery;
    private final ImageQuery imageQuery;
    private final InsectAggregateQuery insectAggregateQuery;

    InsectQueryImpl(SpeciesQuery speciesQuery, ImageQuery imageQuery) {
        Observer.forClass(InsectQueryImpl.class).arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(imageQuery, "imageQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
        InsectAggregateFactory factory = new InsectAggregateFactory(speciesQuery, imageQuery);
        this.insectAggregateQuery = new InsectAggregateQueryImpl(factory);
    }

    @Override
    public InsectAggregateQuery insect() {
        return insectAggregateQuery;
    }

    @Override
    public SpeciesQuery species() {
        return speciesQuery;
    }

    @Override
    public ImageQuery images() {
        return imageQuery;
    }
}
