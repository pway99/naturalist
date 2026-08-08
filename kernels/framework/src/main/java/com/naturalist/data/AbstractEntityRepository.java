package com.naturalist.data;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Observer;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter base class for {@link EntityRepository}. Owns the validation layer
 * and the {@link Observer} scoped to the concrete adapter class. Concrete adapters
 * implement protected template hooks; the public methods are {@code final}, so
 * validation cannot be accidentally skipped.
 *
 * <p>The {@code NAME} bound is open so this adapter serves both slug-keyed and
 * UUID-keyed entity flavors with one implementation. Name-key validity (slug format
 * or UUID presence) is enforced by the name type's own constructor; the boundary
 * check here is null-only.
 *
 * <p>{@code doSave} has no default implementation here — unlike {@code doInsert}/
 * {@code doUpdate}, resolving it correctly requires knowledge of the adapter's own
 * unique constraints (see {@link EntityRepository#save} for the full contract), which
 * this class does not have. {@link AbstractTestEntityRepository} supplies the
 * in-memory implementation by delegating to {@code TestEntitySource#save}, which does
 * have that knowledge via {@code uniqueConstraints()}.
 *
 * @param <NAME>   the entity's name type
 * @param <ENTITY> the named entity type
 */
public abstract class AbstractEntityRepository<
        NAME,
        ENTITY extends Named<NAME>> implements EntityRepository<NAME, ENTITY> {

    private final Observer observer = Observer.forClass(getClass());

    protected final Observer observer() {
        return observer;
    }

    // -----------------------------------------------------------------------------
    // Adapter hooks — implemented by concrete adapters
    // -----------------------------------------------------------------------------

    protected abstract Optional<ENTITY> doGetByName(NAME name);

    protected abstract List<ENTITY> doGetByNameSet(Set<NAME> nameSet);

    protected abstract Page<ENTITY> doGetPage(PageRequest pageRequest);

    protected abstract void doInsert(ENTITY entity);

    protected abstract void doUpdate(ENTITY entity);

    protected abstract ENTITY doSave(ENTITY entity);

    // -----------------------------------------------------------------------------
    // Validated public API — final so subclasses cannot bypass the observer
    // -----------------------------------------------------------------------------

    @Override
    public final Optional<ENTITY> getByName(NAME name) {
        observer.arguments("getByName", i -> i.identifier(name, "name")).throwWhenInvalid();
        return doGetByName(name);
    }

    @Override
    public final List<ENTITY> getByEntityNameSet(Set<NAME> nameSet) {
        observer.arguments("getByEntityNameSet", i -> i.identifierSet(nameSet, "nameSet")).throwWhenInvalid();
        return doGetByNameSet(nameSet);
    }

    @Override
    public final Page<ENTITY> getPage(PageRequest pageRequest) {
        observer.arguments("getPage", i -> i.valueObject(pageRequest, "pageRequest")).throwWhenInvalid();
        return doGetPage(pageRequest);
    }

    @Override
    public final void insert(ENTITY entity) {
        observer.arguments("insert", i -> i.namedEntity(entity, "entity")).throwWhenInvalid();
        doInsert(entity);
    }

    @Override
    public final void update(ENTITY entity) {
        observer.arguments("update", i -> i.namedEntity(entity, "entity")).throwWhenInvalid();
        doUpdate(entity);
    }

    @Override
    public final ENTITY save(ENTITY entity) {
        observer.arguments("save", i -> i.namedEntity(entity, "entity")).throwWhenInvalid();
        return doSave(entity);
    }
}
