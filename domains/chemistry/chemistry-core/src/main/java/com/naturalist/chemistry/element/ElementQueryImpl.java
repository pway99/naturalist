package com.naturalist.chemistry.element;

import com.naturalist.data.AbstractNamedEntityQuery;
import com.naturalist.infrastructure.DomainService;

import java.util.Set;

@DomainService
class ElementQueryImpl
        extends AbstractNamedEntityQuery<ElementName, Element, ElementCollection, ElementRepository.ElementEntityRepository>
        implements ElementQuery {

    ElementQueryImpl(ElementRepository.ElementEntityRepository repository) {
        super(repository);
    }

    @Override
    public ElementCollection findByNameSet(Set<ElementName> elementNames) {
        observer().arguments("findByNameSet", i -> i
                .notNull(elementNames, "elementNames"))
                .throwWhenInvalid();

        return new ElementCollection(repository().getByEntityNameSet(elementNames));
    }
}
