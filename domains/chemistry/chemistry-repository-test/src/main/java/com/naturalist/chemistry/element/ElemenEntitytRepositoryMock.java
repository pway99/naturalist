package com.naturalist.chemistry.element;

import com.naturalist.data.AbstractTestNamedEntityRepository;
import com.naturalist.data.NaturalistDatabase;

class ElemenEntitytRepositoryMock
        extends AbstractTestNamedEntityRepository<ElementName, Element, ElementTestEntitySource>
        implements ElementRepository.ElementEntityRepository {

    protected ElemenEntitytRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
