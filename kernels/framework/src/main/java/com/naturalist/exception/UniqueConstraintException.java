package com.naturalist.exception;

import com.naturalist.ddd.Entity;

public class UniqueConstraintException extends RuntimeException {
    final Entity<?, ?> entity;
    final String name;
    final Object value;

    public UniqueConstraintException(Entity<?,?> entity, String name, Object value) {
        super("%s:: %s is not unique: %s".formatted(entity.getClass().getSimpleName(), name, value));
        this.entity = entity;
        this.name = name;
        this.value = value;
    }

}
