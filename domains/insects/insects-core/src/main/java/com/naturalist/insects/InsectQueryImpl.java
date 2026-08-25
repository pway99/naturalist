package com.naturalist.insects;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.observability.Observer;

import java.util.Optional;

@DomainService
class InsectQueryImpl implements InsectQuery {

    private final SpeciesQuery speciesQuery;
    private final ImageQuery imageQuery;
    private final ObservationQuery observationQuery;
    private final FamilyQuery familyQuery;
    private final GenusQuery genusQuery;
    private final FunctionalRoleQuery functionalRoleQuery;
    private final OrderQuery orderQuery;
    private final CitationQuery citationQuery;
    private final FeatureQuery featureQuery;
    private final InsectFactory insectFactory;

    InsectQueryImpl(SpeciesQuery speciesQuery,
                    ImageQuery imageQuery,
                    FamilyQuery familyQuery,
                    GenusQuery genusQuery,
                    FunctionalRoleQuery functionalRoleQuery,
                    OrderQuery orderQuery,
                    InsectCitationQueryImpl citationQuery,
                    InsectFeatureQueryImpl featureQuery,
                    InsectLifeStageQuery insectLifeStageQuery,
                    ObservationQuery observationQuery) {
        Observer.forClass(InsectQueryImpl.class).arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(imageQuery, "imageQuery")
                        .notNull(familyQuery, "familyQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(functionalRoleQuery, "functionalRoleQuery")
                        .notNull(orderQuery, "orderQuery")
                        .notNull(citationQuery, "citationQuery")
                        .notNull(featureQuery, "featureQuery")
                        .notNull(insectLifeStageQuery, "insectLifeStageQuery")
                        .notNull(observationQuery, "observationQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
        this.observationQuery = observationQuery;
        this.familyQuery = familyQuery;
        this.genusQuery = genusQuery;
        this.functionalRoleQuery = functionalRoleQuery;
        this.orderQuery = orderQuery;
        this.citationQuery = citationQuery;
        this.featureQuery = featureQuery;
        this.insectFactory = new InsectFactory(
                speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery,
                insectLifeStageQuery, citationQuery, featureQuery,
                this.functionalRoleQuery);
    }

    @Override
    public SpeciesQuery species() {
        return speciesQuery;
    }

    @Override
    public ImageQuery images() {
        return imageQuery;
    }

    @Override
    public ObservationQuery observations() {
        return observationQuery;
    }

    @Override
    public FamilyQuery families() {
        return familyQuery;
    }

    @Override
    public GenusQuery genera() {
        return genusQuery;
    }

    @Override
    public FunctionalRoleQuery functionalRoles() {
        return functionalRoleQuery;
    }

    @Override
    public OrderQuery orders() {
        return orderQuery;
    }

    @Override
    public CitationQuery citations() {
        return citationQuery;
    }

    @Override
    public FeatureQuery features() {
        return featureQuery;
    }

    @Override
    public Optional<Insect> getByName(InsectRankName name) {
        return insectFactory.buildByName(name);
    }
}
