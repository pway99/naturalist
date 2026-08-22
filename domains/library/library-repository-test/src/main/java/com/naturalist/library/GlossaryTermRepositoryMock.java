package com.naturalist.library;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

class GlossaryTermRepositoryMock
        extends AbstractTestEntityRepository<GlossaryTermName, GlossaryTerm, GlossaryTermTestEntitySource>
        implements GlossaryTermRepository {

    protected GlossaryTermRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
