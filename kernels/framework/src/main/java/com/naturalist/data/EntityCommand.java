package com.naturalist.data;

import com.naturalist.ddd.Named;

/**
 * Command port for a {@link Named} entity — the symmetric write-side analogue of
 * {@link EntityQuery}. Two public methods, the complete set the domain needs to
 * mutate entities whose identity at the port is their name.
 *
 * <p>The bound on {@code E} is {@link Named} so this port serves both slug-keyed
 * {@link com.naturalist.ddd.NamedEntity} and UUID-keyed {@link com.naturalist.ddd.Entity}
 * flavors with one implementation.
 *
 * <p>This interface is pure vocabulary. Validation and observer plumbing live on
 * {@link AbstractEntityCommand}. Errors from the underlying repository
 * ({@link com.naturalist.exception.PrimaryKeyConstraintException} on duplicate
 * insert, {@link com.naturalist.exception.EntityNotFoundException} on missing
 * update) propagate unchanged — fail-fast, each layer throws what it knows.
 *
 * @param <NAME> the entity's name type
 * @param <E>    the named entity type
 * @see AbstractEntityCommand
 * @see EntityRepository
 */
public interface EntityCommand<NAME, E extends Named<NAME>> {

    void insert(E entity);

    void update(E entity);
}
