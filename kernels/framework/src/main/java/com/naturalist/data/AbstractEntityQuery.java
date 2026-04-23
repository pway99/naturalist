package com.naturalist.data;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.ddd.Named;
import com.naturalist.observability.Observer;

import java.util.Objects;
import java.util.Optional;

/**
 * Framework base class for query adapter implementations in {@code <domain>-core}.
 * Identity at the port is the entity's name (ADR-021).
 *
 * <p>Subclasses implement {@link EntityQuery#findByNameSet(java.util.Set)} directly,
 * wrapping repository results in the domain's concrete {@link BehavioralCollection}. The
 * type parameter makes the wrapping requirement structural (ADR-011).
 *
 * <p><b>Repository type parameter.</b> The {@code R} parameter lets a subclass bind the
 * concrete repository subtype it was constructed with, so {@link #repository()} returns
 * the narrowed type directly. Domain-specific repository methods are reachable without
 * a cast and without a duplicate field in the subclass.
 *
 * @param <NAME> the entity's name type
 * @param <E>    the named entity type
 * @param <EC>   the behavioral collection type returned by multi-result methods
 * @param <R>    the concrete repository subtype backing this query
 */
public abstract class AbstractEntityQuery<
        NAME,
        E extends Named<NAME>,
        EC extends BehavioralCollection<E>,
        R extends EntityRepository<NAME, E>> implements EntityQuery<NAME, E, EC> {

    private final R repository;
    private final Observer observer;

    protected AbstractEntityQuery(R repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.observer = Observer.forClass(getClass());
    }

    protected Observer observer() {
        return observer;
    }

    protected R repository() {
        return repository;
    }

    @Override
    public Optional<E> getByName(NAME name) {
        observer.arguments("getByName", i -> i.notNull(name, "name"))
                .throwWhenInvalid();
        return repository.getByName(name);
    }
}
