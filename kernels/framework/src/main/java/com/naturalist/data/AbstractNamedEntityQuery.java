package com.naturalist.data;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Observer;

import java.util.Objects;
import java.util.Optional;

/**
 * Framework base class for name-only query adapter implementations in
 * {@code <domain>-core}. Parallel to {@link AbstractEntityQuery}, minus every member
 * parameterised by {@link com.naturalist.ddd.PersistenceId}.
 *
 * <p>Subclasses implement {@link NamedEntityQuery#findByNameSet(java.util.Set)} directly,
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
public abstract class AbstractNamedEntityQuery<
        NAME extends EntityName<?>,
        E extends NamedEntity<NAME>,
        EC extends BehavioralCollection<E>,
        R extends NamedEntityRepository<NAME, E>> implements NamedEntityQuery<NAME, E, EC> {

    private final R repository;
    private final Observer observer;

    protected AbstractNamedEntityQuery(R repository) {
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
        observer.arguments("getByName", i -> i.entityName(name, "name"))
                .throwWhenInvalid();
        return repository.getByName(name);
    }
}
