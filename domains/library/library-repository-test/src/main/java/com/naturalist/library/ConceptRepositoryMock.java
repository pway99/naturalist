package com.naturalist.library;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

class ConceptRepositoryMock
        extends AbstractTestEntityRepository<ConceptName, Concept, ConceptTestEntitySource>
        implements ConceptRepository {

    protected ConceptRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
