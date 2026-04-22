package com.naturalist.exception;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Observable;

public class UniqueConstraintException extends RuntimeException {
    final Observable entity;
    final String name;
    final Object value;

    public UniqueConstraintException(Named<?> entity, String name, Object value) {
        super("%s:: %s is not unique: %s".formatted(entity.getClass().getSimpleName(), name, value));
        this.entity = entity;
        this.name = name;
        this.value = value;
    }
}
