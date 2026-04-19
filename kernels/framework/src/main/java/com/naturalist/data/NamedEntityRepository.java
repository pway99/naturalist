package com.naturalist.data;

import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.NamedEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository port for a {@link NamedEntity}. Four public methods — the complete set
 * the domain needs to persist and read entities whose identity at the port is their
 * {@link EntityName}.
 *
 * <p>This interface is pure vocabulary. Template-method hooks and observer plumbing
 * belong to the adapter and live on {@link AbstractNamedEntityRepository}. A caller
 * holding a reference to this port sees the four methods that define the contract;
 * nothing else.
 *
 * @param <NAME>   the entity's name type
 * @param <ENTITY> the named entity type
 * @see AbstractNamedEntityRepository
 * @see com.naturalist.ddd.NamedEntity
 */
public interface NamedEntityRepository<NAME extends EntityName<?>, ENTITY extends NamedEntity<NAME>> {

    Optional<ENTITY> getByName(NAME name);

    List<ENTITY> getByEntityNameSet(Set<NAME> nameSet);

    void insert(ENTITY entity);

    void update(ENTITY entity);
}
