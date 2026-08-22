package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

class CitationRepositoryMock
        extends AbstractTestEntityRepository<CitationName, Citation, CitationTestEntitySource>
        implements CitationRepository {

    protected CitationRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
