package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;

import java.util.Set;

public interface CitationAssociationQuery {

    CitationAssociationCollection findByCitationName(CitationName citationName);

    CitationAssociationCollection findBySubject(EntityRef subject);

    /** Batched sibling of {@link #findBySubject} — resolve every subject's associations in one call. */
    CitationAssociationCollection findBySubjects(Set<EntityRef> subjects);
}
