package com.naturalist.data;

import com.naturalist.ddd.Entity;

import java.util.function.Function;

public interface UniqueConstraint<ENTITY extends Entity<?, ?>> {
    String name();

    Function<ENTITY, ?> valueFunction();

    default Object value(ENTITY entity) {
        return valueFunction().apply(entity);
    }
}
