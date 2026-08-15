package com.naturalist.chemistry.element;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class ElementEntityRepositoryMock
        extends AbstractTestEntityRepository<ElementName, Element, ElementTestEntitySource>
        implements ElementRepository {

    protected ElementEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
