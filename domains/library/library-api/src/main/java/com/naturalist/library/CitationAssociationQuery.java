package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;

public interface CitationAssociationQuery {

    CitationAssociationCollection findByCitationName(CitationName citationName);

    CitationAssociationCollection findBySubject(EntityRef subject);
}
