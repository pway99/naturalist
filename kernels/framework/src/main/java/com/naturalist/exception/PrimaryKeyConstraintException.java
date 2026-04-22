package com.naturalist.exception;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Observable;

public class PrimaryKeyConstraintException extends RuntimeException {
    final Observable entity;

    public PrimaryKeyConstraintException(Named<?> entity) {
        super("Duplicate Primary Key: %s".formatted(entity.name()));
        this.entity = entity;
    }
}
