package com.naturalist.plants;

import com.naturalist.observability.Observer;

class PlantQueryImpl implements PlantQuery {

    private final SpeciesQuery plantEntityQuery;
    private final OrderQuery plantOrderEntityQuery;
    private final FamilyQuery plantFamilyEntityQuery;
    private final GenusQuery plantGenusEntityQuery;
    private final EcologicalRoleQuery plantEcologicalRoleEntityQuery;
    private final FieldObservationQuery fieldObservationQuery;
    private final ImageQuery imageQuery;

    PlantQueryImpl(SpeciesQuery plantEntityQuery,
                   OrderQuery plantOrderEntityQuery,
                   FamilyQuery plantFamilyEntityQuery,
                   GenusQuery plantGenusEntityQuery,
                   EcologicalRoleQuery plantEcologicalRoleEntityQuery,
                   FieldObservationQuery fieldObservationQuery,
                   ImageQuery imageQuery) {
        Observer.forClass(PlantQueryImpl.class).arguments("constructor", i -> i
                        .notNull(plantEntityQuery, "plantEntityQuery")
                        .notNull(plantOrderEntityQuery, "plantOrderEntityQuery")
                        .notNull(plantFamilyEntityQuery, "plantFamilyEntityQuery")
                        .notNull(plantGenusEntityQuery, "plantGenusEntityQuery")
                        .notNull(plantEcologicalRoleEntityQuery, "plantEcologicalRoleEntityQuery")
                        .notNull(fieldObservationQuery, "fieldObservationQuery")
                        .notNull(imageQuery, "imageQuery"))
                .throwWhenInvalid();
        this.plantEntityQuery = plantEntityQuery;
        this.plantOrderEntityQuery = plantOrderEntityQuery;
        this.plantFamilyEntityQuery = plantFamilyEntityQuery;
        this.plantGenusEntityQuery = plantGenusEntityQuery;
        this.plantEcologicalRoleEntityQuery = plantEcologicalRoleEntityQuery;
        this.fieldObservationQuery = fieldObservationQuery;
        this.imageQuery = imageQuery;
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

    @Override
    public FieldObservationQuery fieldObservations() {
        return fieldObservationQuery;
    }

    @Override
    public ImageQuery images() {
        return imageQuery;
    }
}
