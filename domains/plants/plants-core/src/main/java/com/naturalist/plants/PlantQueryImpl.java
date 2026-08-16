package com.naturalist.plants;

import com.naturalist.observability.Observer;

class PlantQueryImpl implements PlantQuery {

    private final PlantEntityQuery plantEntityQuery;
    private final PlantOrderEntityQuery plantOrderEntityQuery;
    private final PlantFamilyEntityQuery plantFamilyEntityQuery;
    private final PlantGenusEntityQuery plantGenusEntityQuery;
    private final PlantEcologicalRoleEntityQuery plantEcologicalRoleEntityQuery;

    PlantQueryImpl(PlantEntityQuery plantEntityQuery,
                   PlantOrderEntityQuery plantOrderEntityQuery,
                   PlantFamilyEntityQuery plantFamilyEntityQuery,
                   PlantGenusEntityQuery plantGenusEntityQuery,
                   PlantEcologicalRoleEntityQuery plantEcologicalRoleEntityQuery) {
        Observer.forClass(PlantQueryImpl.class).arguments("constructor", i -> i
                        .notNull(plantEntityQuery, "plantEntityQuery")
                        .notNull(plantOrderEntityQuery, "plantOrderEntityQuery")
                        .notNull(plantFamilyEntityQuery, "plantFamilyEntityQuery")
                        .notNull(plantGenusEntityQuery, "plantGenusEntityQuery")
                        .notNull(plantEcologicalRoleEntityQuery, "plantEcologicalRoleEntityQuery"))
                .throwWhenInvalid();
        this.plantEntityQuery = plantEntityQuery;
        this.plantOrderEntityQuery = plantOrderEntityQuery;
        this.plantFamilyEntityQuery = plantFamilyEntityQuery;
        this.plantGenusEntityQuery = plantGenusEntityQuery;
        this.plantEcologicalRoleEntityQuery = plantEcologicalRoleEntityQuery;
    }

    @Override
    public PlantEntityQuery plants() {
        return plantEntityQuery;
    }

    @Override
    public PlantOrderEntityQuery orders() {
        return plantOrderEntityQuery;
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
