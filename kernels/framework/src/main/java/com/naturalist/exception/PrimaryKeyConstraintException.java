package com.naturalist.exception;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Observable;

public class PrimaryKeyConstraintException extends RuntimeException {
    final Observable entity;

    public PrimaryKeyConstraintException(Entity<?, ?> entity) {
        super("Duplicate Primary Key: %s".formatted(entity.id()));
        this.entity = entity;
    }

    public PrimaryKeyConstraintException(NamedEntity<?> entity) {
        super("Duplicate Primary Key: %s".formatted(entity.name()));
        this.entity = entity;
    }
}
