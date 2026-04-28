package com.naturalist.plants;

import com.naturalist.observability.Observer;

class PlantQueryImpl implements PlantQuery {

    private final PlantEntityQuery plantEntityQuery;

    PlantQueryImpl(PlantEntityQuery plantEntityQuery) {
        Observer.forClass(PlantQueryImpl.class).arguments("constructor", i -> i
                        .notNull(plantEntityQuery, "plantEntityQuery"))
                .throwWhenInvalid();
        this.plantEntityQuery = plantEntityQuery;
    }

    @Override
    public PlantEntityQuery plants() {
        return plantEntityQuery;
    }
}
