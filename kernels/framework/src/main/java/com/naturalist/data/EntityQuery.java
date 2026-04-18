package com.naturalist.data;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.PersistenceId;

import java.util.Optional;
import java.util.Set;

public interface EntityQuery<ID extends PersistenceId<?>, NAME extends EntityName<?>, E extends Entity<ID, NAME>, EC extends BehavioralCollection<E>> {
    Optional<E> getByName(NAME name);

    Optional<E> getById(ID id);

    EC findByNameSet(Set<NAME> nameSet);

    EC findByIdSet(Set<ID> idSet);
}
