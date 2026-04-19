package com.naturalist.data;

import com.naturalist.observability.Observable;

import java.util.function.Function;

public interface UniqueConstraint<ENTITY extends Observable> {
    String name();

    Function<ENTITY, ?> valueFunction();

    default Object value(ENTITY entity) {
        return valueFunction().apply(entity);
    }
}
