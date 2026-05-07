package com.naturalist.data;

import com.naturalist.ddd.Named;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository port for a {@link Named} entity — any domain record carrying a typed name.
 * Four public methods — the complete set the domain needs to persist and read entities
 * whose identity at the port is their name.
 *
 * <p>The bound on {@code ENTITY} is {@link Named} so this port serves both slug-keyed
 * {@link com.naturalist.ddd.NamedEntity} and UUID-keyed {@link com.naturalist.ddd.Entity}
 * flavors with one implementation.
 *
 * <p>This interface is pure vocabulary. Template-method hooks and observer plumbing
 * belong to the adapter and live on {@link AbstractEntityRepository}. A caller
 * holding a reference to this port sees the four methods that define the contract;
 * nothing else.
 *
 * @param <NAME>   the entity's name type
 * @param <ENTITY> the named entity type
 * @see AbstractEntityRepository
 * @see com.naturalist.ddd.Named
 */
public interface EntityRepository<NAME, ENTITY extends Named<NAME>> {

    Optional<ENTITY> getByName(NAME name);

    List<ENTITY> getByEntityNameSet(Set<NAME> nameSet);

    Page<ENTITY> getPage(PageRequest pageRequest);

    void insert(ENTITY entity);

    void update(ENTITY entity);
}
