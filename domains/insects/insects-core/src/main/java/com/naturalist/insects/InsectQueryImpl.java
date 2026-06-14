package com.naturalist.insects;

import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.observability.Observer;

class InsectQueryImpl implements InsectQuery {

    private final SpeciesQuery speciesQuery;
    private final ImageQuery imageQuery;
    private final FamilyQuery familyQuery;
    private final GenusQuery genusQuery;
    private final FunctionalRoleQuery functionalRoleQuery;
    private final OrderQuery orderQuery;
    private final TaxonViewQuery taxonViewQuery;
    private final CitationQuery citationQuery;

    InsectQueryImpl(SpeciesQuery speciesQuery,
                    ImageQuery imageQuery,
                    FamilyQuery familyQuery,
                    GenusQuery genusQuery,
                    FunctionalRoleQuery functionalRoleQuery,
                    OrderQuery orderQuery,
                    CitationAssociationQuery citationAssociationQuery) {
        Observer.forClass(InsectQueryImpl.class).arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(imageQuery, "imageQuery")
                        .notNull(familyQuery, "familyQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(functionalRoleQuery, "functionalRoleQuery")
                        .notNull(orderQuery, "orderQuery")
                        .notNull(citationAssociationQuery, "citationAssociationQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
        this.familyQuery = familyQuery;
        this.genusQuery = genusQuery;
        this.functionalRoleQuery = functionalRoleQuery;
        this.orderQuery = orderQuery;
        InsectTaxonViewFactory factory =
                new InsectTaxonViewFactory(speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery);
        this.taxonViewQuery = new TaxonViewQueryImpl(factory);
        this.citationQuery = new InsectCitationQueryImpl(
                citationAssociationQuery, speciesQuery, genusQuery, familyQuery);
    }

    @Override
    public TaxonViewQuery taxonView() {
        return taxonViewQuery;
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
}
