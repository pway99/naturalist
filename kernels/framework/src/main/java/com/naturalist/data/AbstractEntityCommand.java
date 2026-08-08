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
 * validation. The {@code doInsert} / {@code doUpdate} / {@code doSave} hooks are
 * validation-free — a trivial command is a one-line subclass with a constructor; an
 * orchestrating command overrides a hook without re-declaring validation.
 *
 * <p>Errors from the repository layer
 * ({@link com.naturalist.exception.PrimaryKeyConstraintException} on duplicate
 * insert, {@link com.naturalist.exception.EntityNotFoundException} on missing
 * update) propagate unchanged from {@code insert}/{@code update}. {@code save} is
 * the method that deliberately avoids provoking either: the default {@code doSave}
 * is a thin pass-through to {@link EntityRepository#save}, which is where the
 * insert-vs-update (and, for an entity with its own declared unique constraints,
 * update-the-matching-row-instead-of-inserting-a-duplicate) resolution actually
 * happens — the repository layer is where that constraint knowledge already lives
 * ({@code TestEntitySource#uniqueConstraints()} today; a real upsert/merge in a
 * future RDBMS adapter). This command layer never attempts an insert or update
 * and catches the other's constraint exception to recover — a constraint
 * violation under a real transactional provider already marks the transaction
 * rollback-only by the time it is caught, so catching it to choose the other
 * branch wouldn't recover anything; it would only defer the failure to commit
 * time.
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

    @Override
    public final E save(E entity) {
        observer.arguments("save", i -> i.namedEntity(entity, "entity")).throwWhenInvalid();
        return doSave(entity);
    }

    protected void doInsert(E entity) {
        repository.insert(entity);
    }

    protected void doUpdate(E entity) {
        repository.update(entity);
    }

    protected E doSave(E entity) {
        return repository.save(entity);
    }
}
