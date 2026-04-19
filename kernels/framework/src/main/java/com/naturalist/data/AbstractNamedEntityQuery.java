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
 * @param <NAME> the entity's name type
 * @param <E>    the named entity type
 * @param <EC>   the behavioral collection type returned by multi-result methods
 */
public abstract class AbstractNamedEntityQuery<
        NAME extends EntityName<?>,
        E extends NamedEntity<NAME>,
        EC extends BehavioralCollection<E>> implements NamedEntityQuery<NAME, E, EC> {

    private final NamedEntityRepository<NAME, E> repository;
    private final Observer observer;

    protected AbstractNamedEntityQuery(NamedEntityRepository<NAME, E> repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.observer = Observer.forClass(getClass());
    }

    protected Observer observer() {
        return observer;
    }

    protected NamedEntityRepository<NAME, E> repository() {
        return repository;
    }

    @Override
    public Optional<E> getByName(NAME name) {
        observer.arguments("getByName", i -> i.entityName(name, "name"))
                .throwWhenInvalid();
        return repository.getByName(name);
    }
}
