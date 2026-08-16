package com.naturalist.plants;

import com.naturalist.observability.Observer;

class PlantQueryImpl implements PlantQuery {

    private final PlantEntityQuery plantEntityQuery;
    private final PlantFamilyEntityQuery plantFamilyEntityQuery;
    private final PlantGenusEntityQuery plantGenusEntityQuery;
    private final PlantEcologicalRoleEntityQuery plantEcologicalRoleEntityQuery;

    PlantQueryImpl(PlantEntityQuery plantEntityQuery,
                   PlantFamilyEntityQuery plantFamilyEntityQuery,
                   PlantGenusEntityQuery plantGenusEntityQuery,
                   PlantEcologicalRoleEntityQuery plantEcologicalRoleEntityQuery) {
        Observer.forClass(PlantQueryImpl.class).arguments("constructor", i -> i
                        .notNull(plantEntityQuery, "plantEntityQuery")
                        .notNull(plantFamilyEntityQuery, "plantFamilyEntityQuery")
                        .notNull(plantGenusEntityQuery, "plantGenusEntityQuery")
                        .notNull(plantEcologicalRoleEntityQuery, "plantEcologicalRoleEntityQuery"))
                .throwWhenInvalid();
        this.plantEntityQuery = plantEntityQuery;
        this.plantFamilyEntityQuery = plantFamilyEntityQuery;
        this.plantGenusEntityQuery = plantGenusEntityQuery;
        this.plantEcologicalRoleEntityQuery = plantEcologicalRoleEntityQuery;
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

    @Override
    public PlantEcologicalRoleEntityQuery ecologicalRoles() {
        return plantEcologicalRoleEntityQuery;
    }
}
