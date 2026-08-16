package com.naturalist.plants;

import com.naturalist.observability.Observer;

class PlantQueryImpl implements PlantQuery {

    private final SpeciesQuery plantEntityQuery;
    private final OrderQuery plantOrderEntityQuery;
    private final FamilyQuery plantFamilyEntityQuery;
    private final GenusQuery plantGenusEntityQuery;
    private final EcologicalRoleQuery plantEcologicalRoleEntityQuery;

    PlantQueryImpl(SpeciesQuery plantEntityQuery,
                   OrderQuery plantOrderEntityQuery,
                   FamilyQuery plantFamilyEntityQuery,
                   GenusQuery plantGenusEntityQuery,
                   EcologicalRoleQuery plantEcologicalRoleEntityQuery) {
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
    public SpeciesQuery species() {
        return plantEntityQuery;
    }

    @Override
    public OrderQuery orders() {
        return plantOrderEntityQuery;
    }

    @Override
    public FamilyQuery families() {
        return plantFamilyEntityQuery;
    }

    @Override
    public GenusQuery genera() {
        return plantGenusEntityQuery;
    }

    @Override
    public EcologicalRoleQuery ecologicalRoles() {
        return plantEcologicalRoleEntityQuery;
    }
}
