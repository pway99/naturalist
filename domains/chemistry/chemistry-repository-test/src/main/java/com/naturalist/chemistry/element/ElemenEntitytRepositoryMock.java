package com.naturalist.chemistry.element;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Observer;

class ElemenEntitytRepositoryMock extends AbstractTestEntityRepository<ElementId, ElementName, Element, ElementTestEntitySource>
    implements ElementRepository.ElementEntityRepository {
    private static final Observer observer = Observer.forClass(ElemenEntitytRepositoryMock.class);

    protected ElemenEntitytRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Observer observer() {
        return observer;
    }
}
