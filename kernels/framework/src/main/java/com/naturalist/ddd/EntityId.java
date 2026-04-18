package com.naturalist.ddd;

/**
 * @deprecated Renamed to {@link PersistenceId}. This alias will be removed once
 * the concept of EntityId is reconsidered and assigned a new meaning.
 */
@Deprecated
public abstract class EntityId<T> extends PersistenceId<T> {
    protected EntityId(T value) {
        super(value);
    }
}
