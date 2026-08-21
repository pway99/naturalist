package com.naturalist.plants;

import com.naturalist.observability.Observer;

import java.util.Optional;

class PlantQueryImpl implements PlantQuery {

    private final SpeciesQuery plantEntityQuery;
    private final OrderQuery plantOrderEntityQuery;
    private final FamilyQuery plantFamilyEntityQuery;
    private final GenusQuery plantGenusEntityQuery;
    private final EcologicalRoleQuery plantEcologicalRoleEntityQuery;
    private final ObservationQuery observationQuery;
    private final ImageQuery imageQuery;
    private final FeatureQuery featureQuery;
    private final PlantFactory plantFactory;

    PlantQueryImpl(SpeciesQuery plantEntityQuery,
                   OrderQuery plantOrderEntityQuery,
                   FamilyQuery plantFamilyEntityQuery,
                   GenusQuery plantGenusEntityQuery,
                   EcologicalRoleQuery plantEcologicalRoleEntityQuery,
                   ObservationQuery observationQuery,
                   ImageQuery imageQuery,
                   FeatureQuery featureQuery) {
        Observer.forClass(PlantQueryImpl.class).arguments("constructor", i -> i
                        .notNull(plantEntityQuery, "plantEntityQuery")
                        .notNull(plantOrderEntityQuery, "plantOrderEntityQuery")
                        .notNull(plantFamilyEntityQuery, "plantFamilyEntityQuery")
                        .notNull(plantGenusEntityQuery, "plantGenusEntityQuery")
                        .notNull(plantEcologicalRoleEntityQuery, "plantEcologicalRoleEntityQuery")
                        .notNull(observationQuery, "observationQuery")
                        .notNull(imageQuery, "imageQuery")
                        .notNull(featureQuery, "featureQuery"))
                .throwWhenInvalid();
        this.plantEntityQuery = plantEntityQuery;
        this.plantOrderEntityQuery = plantOrderEntityQuery;
        this.plantFamilyEntityQuery = plantFamilyEntityQuery;
        this.plantGenusEntityQuery = plantGenusEntityQuery;
        this.plantEcologicalRoleEntityQuery = plantEcologicalRoleEntityQuery;
        this.observationQuery = observationQuery;
        this.imageQuery = imageQuery;
        this.featureQuery = featureQuery;
        this.plantFactory = new PlantFactory(
                plantEntityQuery, plantGenusEntityQuery, plantFamilyEntityQuery, plantOrderEntityQuery, featureQuery,
                plantEcologicalRoleEntityQuery, imageQuery);
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
    public ObservationQuery observations() {
        return observationQuery;
    }

    @Override
    public ImageQuery images() {
        return imageQuery;
    }

    @Override
    public FeatureQuery features() {
        return featureQuery;
    }

    @Override
    public Optional<Plant> getByName(PlantRankName name) {
        return plantFactory.buildByName(name);
    }
}
