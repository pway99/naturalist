package com.naturalist.insects;

import com.naturalist.observability.Observer;

import java.util.Optional;

class InsectAggregateQueryImpl implements InsectQuery.InsectAggregateQuery {

    private final Observer observer = Observer.forClass(getClass());
    private final InsectAggregateFactory factory;

    InsectAggregateQueryImpl(InsectAggregateFactory factory) {
        this.factory = factory;
    }

    @Override
    public Optional<InsectAggregate> getByName(InsectSpeciesName name) {
        observer.arguments("getByName", i -> i.entityName(name, "name")).throwWhenInvalid();
        return factory.buildByName(name);
    }
}
