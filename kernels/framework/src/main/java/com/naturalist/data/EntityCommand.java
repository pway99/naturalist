package com.naturalist.data;

import com.naturalist.ddd.Named;

/**
 * Command port for a {@link Named} entity — the symmetric write-side analogue of
 * {@link EntityQuery}. Three public methods, the complete set the domain needs to
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
 * update) propagate unchanged from {@code insert}/{@code update} — fail-fast, each
 * layer throws what it knows. {@code save} exists specifically to avoid provoking
 * either: callers that don't know whether an entity already exists (idempotent
 * writes, re-attribution, transactional aggregate builders) should call
 * {@code save} rather than guess between {@code insert} and {@code update} and
 * catch the constraint exception. Under a real transactional provider, catching a
 * constraint violation to recover is unsafe — the transaction is already marked
 * rollback-only by the time the exception is visible to the caller — so
 * {@code save} delegates the resolution to {@link EntityRepository#save}, which
 * decides before either write is attempted. See that method for the exact
 * insert/update/reconcile semantics.
 *
 * @param <NAME> the entity's name type
 * @param <E>    the named entity type
 * @see AbstractEntityCommand
 * @see EntityRepository
 */
public interface EntityCommand<NAME, E extends Named<NAME>> {

    void insert(E entity);

    void update(E entity);

    /**
     * Insert-or-update, resolved without provoking
     * {@link com.naturalist.exception.PrimaryKeyConstraintException} or
     * {@link com.naturalist.exception.EntityNotFoundException}. See
     * {@link EntityRepository#save} for the full resolution contract, including
     * how a match on a declared unique constraint (rather than the entity's own
     * key) is handled.
     *
     * <p>Returns the entity actually persisted — see {@link EntityRepository#save}
     * for why that can differ from {@code entity} itself, and why a caller building
     * a dependent record must use the returned value rather than the argument.
     */
    E save(E entity);
}
