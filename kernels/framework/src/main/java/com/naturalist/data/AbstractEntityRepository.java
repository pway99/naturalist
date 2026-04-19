package com.naturalist.data;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.PersistenceId;
import com.naturalist.observability.Observer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter base class for {@link EntityRepository}. Owns the validation layer and the
 * {@link Observer} scoped to the concrete adapter class. Concrete adapters implement
 * six protected template hooks; the public methods are {@code final}, so validation
 * cannot be accidentally skipped.
 *
 * <p>The {@link Observer} is scoped with {@code getClass()}, so diagnostic messages
 * carry the concrete adapter's simple name without any work on the subclass's part.
 *
 * @param <ID>     the persistence identifier type
 * @param <NAME>   the entity name type
 * @param <ENTITY> the entity type
 */
public abstract class AbstractEntityRepository<
        ID extends PersistenceId<?>,
        NAME extends EntityName<?>,
        ENTITY extends Entity<ID, NAME>> implements EntityRepository<ID, NAME, ENTITY> {

    private final Observer observer = Observer.forClass(getClass());

    protected final Observer observer() {
        return observer;
    }

    // -----------------------------------------------------------------------------
    // Adapter hooks — implemented by concrete adapters
    // -----------------------------------------------------------------------------

    protected abstract Optional<ENTITY> doGetById(ID id);

    protected abstract Optional<ENTITY> doGetByName(NAME name);

    protected abstract List<ENTITY> doGetByIdSet(Set<ID> idSet);

    protected abstract List<ENTITY> doGetByNameSet(Set<NAME> nameSet);

    protected abstract void doInsert(ENTITY entity);

    protected abstract void doUpdate(ENTITY entity);

    // -----------------------------------------------------------------------------
    // Validated public API — final so subclasses cannot bypass the observer
    // -----------------------------------------------------------------------------

    @Override
    public final Optional<ENTITY> getById(ID id) {
        observer.arguments("getById", i -> i.notNull(id, "id")).throwWhenInvalid();
        return doGetById(id);
    }

    @Override
    public final Optional<ENTITY> getByName(NAME name) {
        observer.arguments("getByName", i -> i.entityName(name, "name")).throwWhenInvalid();
        return doGetByName(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final List<ENTITY> getByIdSet(Set<ID> idSet) {
        observer.arguments("getByIdSet",
                        i -> i.entityIdCollection((Collection<PersistenceId<?>>) idSet, "idSet"))
                .throwWhenInvalid();
        return doGetByIdSet(idSet);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final List<ENTITY> getByEntityNameSet(Set<NAME> nameSet) {
        observer.arguments("getByEntityNameSet",
                        i -> i.entityNameCollection((Collection<NAME>) nameSet, "nameSet"))
                .throwWhenInvalid();
        return doGetByNameSet(nameSet);
    }

    @Override
    public final void insert(ENTITY entity) {
        observer.arguments("insert", i -> i.entity(entity, "entity")).throwWhenInvalid();
        doInsert(entity);
    }

    @Override
    public final void update(ENTITY entity) {
        observer.arguments("update", i -> i.entity(entity, "entity")).throwWhenInvalid();
        doUpdate(entity);
    }
}
