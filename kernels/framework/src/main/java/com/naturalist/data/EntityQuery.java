package com.naturalist.data;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.PersistenceId;

import java.util.Set;

/**
 * Query port for a single {@link Entity} type.
 *
 * <p>Single-entity lookups ({@code getById}, {@code getByName}) and the polymorphic
 * {@link #get(com.naturalist.ddd.Identifier)} dispatch are inherited from {@link Query}.
 * This interface adds the multi-result methods that return a domain-specific
 * {@link BehavioralCollection}.
 */
public interface EntityQuery<ID extends PersistenceId<?>, NAME extends EntityName<?>, E extends Entity<ID, NAME>, EC extends BehavioralCollection<E>>
        extends Query<ID, NAME, E> {

    EC findByNameSet(Set<NAME> nameSet);

    EC findByIdSet(Set<ID> idSet);
}
