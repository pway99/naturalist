package com.naturalist.insects;

class InsectQueryImpl implements InsectQuery {
    final SpeciesQuery speciesQuery;
    final ImageQuery imageQuery;
    final InsectAggregateQuery insectAggregateQuery;

    InsectQueryImpl(SpeciesQuery speciesQuery, ImageQuery imageQuery) {
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
        this.insectAggregateQuery = new InsectAggregateQueryImpl(speciesQuery, imageQuery);
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
