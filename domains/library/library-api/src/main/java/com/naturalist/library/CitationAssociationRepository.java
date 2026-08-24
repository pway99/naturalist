package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.EntityRepository;

import java.util.List;
import java.util.Set;

interface CitationAssociationRepository
        extends EntityRepository<CitationAssociationId, CitationAssociation> {

    List<CitationAssociation> getByCitationName(CitationName citationName);

    List<CitationAssociation> getBySubject(EntityRef subject);

    /** Batched sibling of {@link #getBySubject} — resolve every subject's associations in one call. */
    List<CitationAssociation> getBySubjects(Set<EntityRef> subjects);
}
