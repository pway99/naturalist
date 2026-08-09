package com.naturalist.library;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class GlossaryTermRepositoryMock
        extends AbstractTestEntityRepository<GlossaryTermName, GlossaryTerm, GlossaryTermTestEntitySource>
        implements GlossaryTermRepository {

    protected GlossaryTermRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
