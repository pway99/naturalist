package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

@DomainService
class CitationAssociationQueryImpl implements CitationAssociationQuery {

    private static final Observer observer = Observer.forClass(CitationAssociationQueryImpl.class);

    private final CitationAssociationRepository repository;

    CitationAssociationQueryImpl(CitationAssociationRepository repository) {
        this.repository = repository;
    }

    @Override
    public CitationAssociationCollection findByCitationName(CitationName citationName) {
        observer.arguments("findByCitationName", i -> i
                        .entityName(citationName, "citationName"))
                .throwWhenInvalid();
        return CitationAssociationCollection.of(repository.getByCitationName(citationName));
    }

    @Override
    public CitationAssociationCollection findBySubject(EntityRef subject) {
        observer.arguments("findBySubject", i -> i
                        .valueObject(subject, "subject"))
                .throwWhenInvalid();
        return CitationAssociationCollection.of(repository.getBySubject(subject));
    }
}
