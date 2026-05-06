package com.naturalist.exception;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Observable;

public class ForeignKeyConstraintException extends RuntimeException {
    final Observable entity;
    final String name;
    final Object foreignValue;

    public ForeignKeyConstraintException(Named<?> entity, String name, Object foreignValue) {
        super("%s:: %s references unknown %s".formatted(entity.getClass().getSimpleName(), name, foreignValue));
        this.entity = entity;
        this.name = name;
        this.foreignValue = foreignValue;
    }
}
