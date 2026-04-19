package com.naturalist.exception;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Observable;

public class EntityNotFoundException extends RuntimeException {
    final Observable entity;

    public EntityNotFoundException(Entity<?, ?> entity) {
        super("Entity Not Found: %s".formatted(entity.id()));
        this.entity = entity;
    }

    public EntityNotFoundException(NamedEntity<?> entity) {
        super("Entity Not Found: %s".formatted(entity.name()));
        this.entity = entity;
    }

    public EntityNotFoundException(EntityName<?> name) {
        super("Entity Not Found: %s".formatted(name));
        this.entity = null;
    }
}
