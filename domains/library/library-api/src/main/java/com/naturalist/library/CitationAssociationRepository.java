package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.EntityRepository;

import java.util.List;

interface CitationAssociationRepository
        extends EntityRepository<CitationAssociationId, CitationAssociation> {

    List<CitationAssociation> getByCitationName(CitationName citationName);

    List<CitationAssociation> getBySubject(EntityRef subject);
}
