package com.naturalist.data;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.PersistenceId;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Observer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EntityRepository<ID extends PersistenceId<?>, NAME extends EntityName<?>, ENTITY extends Entity<ID, NAME>> {
    Observer observer();

    Optional<ENTITY> doGetById(ID id);

    Optional<ENTITY> doGetByName(NAME name);

    void doInsert(ENTITY entity);

    void doUpdate(ENTITY entity);

    default void insert(ENTITY entity) {
        observer().arguments("insert", i -> i.entity(entity, "entity")).throwWhenInvalid();
        doInsert(entity);
    }

    default void update(ENTITY entity) {
        observer().arguments("update", i -> i.entity(entity, "entity")).throwWhenInvalid();
        doUpdate(entity);
    }

    default Optional<ENTITY> getById(ID id) {
        observer().arguments("getById", i -> i
                .notNull(id, "id")
        ).throwWhenInvalid();
        return doGetById(id);
    }

    @SuppressWarnings("unchecked")
    default List<ENTITY> getByIdSet(Set<ID> idSet) {
        observer().arguments("getByIdSet", i -> i
                        .entityIdCollection((Collection<PersistenceId<?>>) idSet, "idSet"))
                .throwWhenInvalid();
        return doGetByIdSet(idSet);
    }
    List<ENTITY> doGetByIdSet(Set<ID> idSet);


    default Optional<ENTITY> getByName(NAME name) {
        observer().arguments("getByNameFunction", i -> i
                .entityName(name, "name")
        ).throwWhenInvalid();
        return doGetByName(name);
    }

    @SuppressWarnings("unchecked")
    default List<ENTITY> getByEntityNameSet(Set<NAME> nameSet) {
        observer().arguments("getByEntityNameSet", i -> i
                        .entityNameCollection((Collection<NAME>) nameSet, "nameSet"))
                .throwWhenInvalid();
        return doGetByNameSet(nameSet);
    }

    List<ENTITY> doGetByNameSet(Set<NAME> nameSet);
}
