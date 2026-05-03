package com.naturalist.chemistry.element;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class ElemenEntitytRepositoryMock
        extends AbstractTestEntityRepository<ElementName, Element, ElementTestEntitySource>
        implements ElementRepository {

    protected ElemenEntitytRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
