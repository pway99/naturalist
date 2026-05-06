package com.naturalist.plants;

import com.naturalist.observability.Observer;

class PlantQueryImpl implements PlantQuery {

    private final PlantEntityQuery plantEntityQuery;
    private final PlantFamilyEntityQuery plantFamilyEntityQuery;
    private final PlantGenusEntityQuery plantGenusEntityQuery;

    PlantQueryImpl(PlantEntityQuery plantEntityQuery,
                   PlantFamilyEntityQuery plantFamilyEntityQuery,
                   PlantGenusEntityQuery plantGenusEntityQuery) {
        Observer.forClass(PlantQueryImpl.class).arguments("constructor", i -> i
                        .notNull(plantEntityQuery, "plantEntityQuery")
                        .notNull(plantFamilyEntityQuery, "plantFamilyEntityQuery")
                        .notNull(plantGenusEntityQuery, "plantGenusEntityQuery"))
                .throwWhenInvalid();
        this.plantEntityQuery = plantEntityQuery;
        this.plantFamilyEntityQuery = plantFamilyEntityQuery;
        this.plantGenusEntityQuery = plantGenusEntityQuery;
    }

    @Override
    public PlantEntityQuery plants() {
        return plantEntityQuery;
    }

    @Override
    public PlantFamilyEntityQuery families() {
        return plantFamilyEntityQuery;
    }

    @Override
    public PlantGenusEntityQuery genera() {
        return plantGenusEntityQuery;
    }
}
