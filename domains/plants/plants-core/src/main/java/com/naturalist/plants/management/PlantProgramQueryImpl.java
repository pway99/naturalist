package com.naturalist.plants.management;

import com.naturalist.observability.Observer;

class PlantProgramQueryImpl implements PlantProgramQuery {

    private final PlantProgramEntityQuery plantProgramEntityQuery;

    PlantProgramQueryImpl(PlantProgramEntityQuery plantProgramEntityQuery) {
        Observer.forClass(PlantProgramQueryImpl.class).arguments("constructor", i -> i
                        .notNull(plantProgramEntityQuery, "plantProgramEntityQuery"))
                .throwWhenInvalid();
        this.plantProgramEntityQuery = plantProgramEntityQuery;
    }

    @Override
    public PlantProgramEntityQuery programs() {
        return plantProgramEntityQuery;
    }
}
