package com.naturalist.chemistry.element;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;

import java.util.Set;

@DomainService
class ElementQueryImpl
        extends AbstractEntityQuery<ElementName, Element, ElementCollection, ElementRepository.ElementEntityRepository>
        implements ElementQuery {

    ElementQueryImpl(ElementRepository.ElementEntityRepository repository) {
        super(repository);
    }

    @Override
    public ElementCollection findByNameSet(Set<ElementName> elementNames) {
        observer().arguments("findByNameSet", i -> i
                .identifierSet(elementNames, "elementNames"))
                .throwWhenInvalid();

        return new ElementCollection(repository().getByEntityNameSet(elementNames));
    }
}
