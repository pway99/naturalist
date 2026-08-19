package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectEntityCollections.OrderCollection;

import java.util.Set;

@DomainService
class InsectOrderQueryImpl
        extends AbstractEntityQuery<
        InsectOrderName,
        InsectOrder,
        OrderCollection,
        InsectRepository.OrderRepository>
        implements InsectQuery.OrderQuery {

    InsectOrderQueryImpl(InsectRepository.OrderRepository repository) {
        super(repository);
    }

    @Override
    public OrderCollection findByNameSet(Set<InsectOrderName> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return OrderCollection.of(repository().getByEntityNameSet(names));
    }
}
