package com.naturalist.data;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.PersistenceId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository port for an {@link Entity}. Six public methods — the complete set the
 * domain needs to persist and read entities with both id- and name-based lookup.
 *
 * <p>This interface is pure vocabulary. It carries no template-method hooks and no
 * observer plumbing — those belong to the adapter and live on
 * {@link AbstractEntityRepository}. A caller holding a reference to this port sees the
 * six methods that define the contract; nothing else.
 *
 * <p>Concrete adapters extend {@link AbstractEntityRepository} to inherit validated
 * implementations of all six methods and supply the adapter-specific read/write hooks.
 *
 * @param <ID>     the persistence identifier type
 * @param <NAME>   the entity name type
 * @param <ENTITY> the entity type
 * @see AbstractEntityRepository
 */
public interface EntityRepository<ID extends PersistenceId<?>, NAME extends EntityName<?>, ENTITY extends Entity<ID, NAME>> {

    Optional<ENTITY> getById(ID id);

    Optional<ENTITY> getByName(NAME name);

    List<ENTITY> getByIdSet(Set<ID> idSet);

    List<ENTITY> getByEntityNameSet(Set<NAME> nameSet);

    void insert(ENTITY entity);

    void update(ENTITY entity);
}
