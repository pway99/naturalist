package com.naturalist.library;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

@MockDomainService
class GlossaryTermRepositoryMock
        extends AbstractTestEntityRepository<GlossaryTermName, GlossaryTerm, GlossaryTermTestEntitySource>
        implements GlossaryTermRepository {

    protected GlossaryTermRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
