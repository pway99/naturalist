package com.naturalist.insects;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.library.CitationAssociationQuery;
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
                    CitationAssociationQuery citationAssociationQuery,
                    com.naturalist.library.CitationQuery libraryCitationQuery,
                    InsectLifeStageQuery insectLifeStageQuery,
                    InsectRepository.FeatureRepository featureRepository,
                    InsectRepository.FeatureAssignmentRepository featureAssignmentRepository,
                    ObservationQuery observationQuery) {
        Observer.forClass(InsectQueryImpl.class).arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(imageQuery, "imageQuery")
                        .notNull(familyQuery, "familyQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(functionalRoleQuery, "functionalRoleQuery")
                        .notNull(orderQuery, "orderQuery")
                        .notNull(citationAssociationQuery, "citationAssociationQuery")
                        .notNull(libraryCitationQuery, "libraryCitationQuery")
                        .notNull(insectLifeStageQuery, "insectLifeStageQuery")
                        .notNull(featureRepository, "featureRepository")
                        .notNull(featureAssignmentRepository, "featureAssignmentRepository")
                        .notNull(observationQuery, "observationQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
        this.observationQuery = observationQuery;
        this.familyQuery = familyQuery;
        this.genusQuery = genusQuery;
        this.functionalRoleQuery = functionalRoleQuery;
        this.orderQuery = orderQuery;
        InsectAncestryResolver ancestryResolver =
                new InsectAncestryResolver(speciesQuery, genusQuery, familyQuery);
        InsectCitationQueryImpl citationQueryImpl = new InsectCitationQueryImpl(
                citationAssociationQuery, libraryCitationQuery, ancestryResolver);
        InsectFeatureQueryImpl featureQueryImpl = new InsectFeatureQueryImpl(
                featureRepository, featureAssignmentRepository, ancestryResolver);
        this.citationQuery = citationQueryImpl;
        this.featureQuery = featureQueryImpl;
        this.insectFactory = new InsectFactory(
                speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery,
                insectLifeStageQuery, citationQueryImpl, featureQueryImpl,
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
