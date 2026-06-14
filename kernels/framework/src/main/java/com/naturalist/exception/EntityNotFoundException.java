package com.naturalist.exception;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Observable;

public class EntityNotFoundException extends RuntimeException {
    final Observable entity;

    public EntityNotFoundException(Named<?> entity) {
        super("Entity Not Found: %s".formatted(entity.key()));
        this.entity = entity;
    }
}
