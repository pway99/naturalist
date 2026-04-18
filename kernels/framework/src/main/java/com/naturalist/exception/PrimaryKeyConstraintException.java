package com.naturalist.exception;

import com.naturalist.ddd.Entity;

public class PrimaryKeyConstraintException extends RuntimeException {
    final Entity<?, ?> entity;

    public PrimaryKeyConstraintException(Entity<?, ?> entity) {
        super("Duplicate Primary Key: %s".formatted(entity.id()));
        this.entity = entity;
    }
}
