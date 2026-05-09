package com.naturalist.data;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Observer;

import java.util.Objects;

/**
 * Adapter base class for {@link EntityCommand} — the symmetric write-side analogue of
 * {@link AbstractEntityQuery}. Owns the validation layer and the {@link Observer}
 * scoped to the concrete adapter class.
 *
 * <p>Public methods are {@code final}, so an adapter cannot accidentally skip
 * validation. The {@code doInsert} / {@code doUpdate} hooks are validation-free —
 * a trivial command is a one-line subclass with a constructor; an orchestrating
 * command overrides the hook without re-declaring validation.
 *
 * <p>Errors from the repository layer
 * ({@link com.naturalist.exception.PrimaryKeyConstraintException} on duplicate
 * insert, {@link com.naturalist.exception.EntityNotFoundException} on missing
 * update) propagate unchanged.
 *
 * @param <NAME> the entity's name type
 * @param <E>    the named entity type
 * @param <R>    the concrete repository subtype backing this command
 */
public abstract class AbstractEntityCommand<
        NAME,
        E extends Named<NAME>,
        R extends EntityRepository<NAME, E>> implements EntityCommand<NAME, E> {

    private final R repository;
    private final Observer observer = Observer.forClass(getClass());

    protected AbstractEntityCommand(R repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    protected R repository() {
        return repository;
    }

    protected Observer observer() {
        return observer;
    }

    @Override
    public final void insert(E entity) {
        observer.arguments("insert", i -> i.namedEntity(entity, "entity")).throwWhenInvalid();
        doInsert(entity);
    }

    @Override
    public final void update(E entity) {
        observer.arguments("update", i -> i.namedEntity(entity, "entity")).throwWhenInvalid();
        doUpdate(entity);
    }

    protected void doInsert(E entity) {
        repository.insert(entity);
    }

    protected void doUpdate(E entity) {
        repository.update(entity);
    }
}
