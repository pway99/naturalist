package com.naturalist.exception;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Observable;

public class UniqueConstraintException extends RuntimeException {
    final Observable entity;
    final String name;
    final Object value;

    public UniqueConstraintException(Entity<?,?> entity, String name, Object value) {
        super("%s:: %s is not unique: %s".formatted(entity.getClass().getSimpleName(), name, value));
        this.entity = entity;
        this.name = name;
        this.value = value;
    }

    public UniqueConstraintException(NamedEntity<?> entity, String name, Object value) {
        super("%s:: %s is not unique: %s".formatted(entity.getClass().getSimpleName(), name, value));
        this.entity = entity;
        this.name = name;
        this.value = value;
    }
}
