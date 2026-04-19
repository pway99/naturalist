package com.naturalist.insects;

class InsectQueryImpl implements InsectQuery {

    private final SpeciesQuery speciesQuery;
    private final ImageQuery imageQuery;
    private final InsectAggregateQuery insectAggregateQuery;

    InsectQueryImpl(SpeciesQuery speciesQuery, ImageQuery imageQuery) {
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
        InsectAggregateFactory factory = new InsectAggregateFactoryImpl(speciesQuery, imageQuery);
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
