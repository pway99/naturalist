package com.naturalist.data;

import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Observer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter base class for {@link NamedEntityRepository}. Owns the validation layer
 * and the {@link Observer} scoped to the concrete adapter class. Concrete adapters
 * implement four protected template hooks; the public methods are {@code final}, so
 * validation cannot be accidentally skipped.
 *
 * @param <NAME>   the entity's name type
 * @param <ENTITY> the named entity type
 */
public abstract class AbstractNamedEntityRepository<
        NAME extends EntityName<?>,
        ENTITY extends NamedEntity<NAME>> implements NamedEntityRepository<NAME, ENTITY> {

    private final Observer observer = Observer.forClass(getClass());

    protected final Observer observer() {
        return observer;
    }

    // -----------------------------------------------------------------------------
    // Adapter hooks — implemented by concrete adapters
    // -----------------------------------------------------------------------------

    protected abstract Optional<ENTITY> doGetByName(NAME name);

    protected abstract List<ENTITY> doGetByNameSet(Set<NAME> nameSet);

    protected abstract void doInsert(ENTITY entity);

    protected abstract void doUpdate(ENTITY entity);

    // -----------------------------------------------------------------------------
    // Validated public API — final so subclasses cannot bypass the observer
    // -----------------------------------------------------------------------------

    @Override
    public final Optional<ENTITY> getByName(NAME name) {
        observer.arguments("getByName", i -> i.entityName(name, "name")).throwWhenInvalid();
        return doGetByName(name);
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
        observer.arguments("insert", i -> i.namedEntity(entity, "entity")).throwWhenInvalid();
        doInsert(entity);
    }

    @Override
    public final void update(ENTITY entity) {
        observer.arguments("update", i -> i.namedEntity(entity, "entity")).throwWhenInvalid();
        doUpdate(entity);
    }
}
