package com.naturalist.chemistry.element;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.data.EntityRepository;
import com.naturalist.infrastructure.DomainService;

import java.util.Set;

@DomainService
class ElementQueryImpl extends AbstractEntityQuery<ElementId, ElementName, Element, ElementCollection> implements ElementQuery {

    ElementQueryImpl(EntityRepository<ElementId, ElementName, Element> repository) {
        super(repository);
    }

    @Override
    public ElementCollection findByNameSet(Set<ElementName> elementNames) {
        observer().arguments("findByNameSet", i -> i
                .notNull(elementNames, "elementNames"))
                .throwWhenInvalid();

        return new ElementCollection(repository().getByEntityNameSet(elementNames));
    }

    @Override
    public ElementCollection findByIdSet(Set<ElementId> elementIds) {
        observer().arguments("findByIdSet", i -> i
                .notNull(elementIds, "elementIds"))
                .throwWhenInvalid();

        return new ElementCollection(repository().getByIdSet(elementIds));
    }
}
