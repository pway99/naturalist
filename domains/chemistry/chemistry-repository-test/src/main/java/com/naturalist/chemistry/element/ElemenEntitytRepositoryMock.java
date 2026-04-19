package com.naturalist.chemistry.element;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

class ElemenEntitytRepositoryMock extends AbstractTestEntityRepository<ElementId, ElementName, Element, ElementTestEntitySource>
    implements ElementRepository.ElementEntityRepository {

    protected ElemenEntitytRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
