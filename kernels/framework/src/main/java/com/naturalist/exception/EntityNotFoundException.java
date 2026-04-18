package com.naturalist.exception;

import com.naturalist.ddd.Entity;

public class EntityNotFoundException extends RuntimeException {
    final Entity<?, ?> entity;

    public EntityNotFoundException(Entity<?, ?> entity) {
        super("Entity Not Found: %s".formatted(entity.id()));
        this.entity = entity;
    }
}
