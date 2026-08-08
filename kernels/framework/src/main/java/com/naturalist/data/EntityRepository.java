package com.naturalist.data;

import com.naturalist.ddd.Named;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository port for a {@link Named} entity — any domain record carrying a typed name.
 * Six public methods — the complete set the domain needs to persist and read entities
 * whose identity at the port is their name.
 *
 * <p>The bound on {@code ENTITY} is {@link Named} so this port serves both slug-keyed
 * {@link com.naturalist.ddd.NamedEntity} and UUID-keyed {@link com.naturalist.ddd.Entity}
 * flavors with one implementation.
 *
 * <p>This interface is pure vocabulary. Template-method hooks and observer plumbing
 * belong to the adapter and live on {@link AbstractEntityRepository}. A caller
 * holding a reference to this port sees the six methods that define the contract;
 * nothing else. {@link #insert} throws
 * {@link com.naturalist.exception.PrimaryKeyConstraintException} on a duplicate key
 * and {@link #update} throws {@link com.naturalist.exception.EntityNotFoundException}
 * on a missing one; {@link #save} is the method that deliberately provokes neither —
 * see its own javadoc for the resolution contract.
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

    /**
     * Insert-or-update, resolved by identity rather than by attempting one of
     * {@link #insert}/{@link #update} and catching the other's constraint
     * exception to recover. Three cases, in order:
     * <ol>
     *   <li>An entity keyed by {@code entity.key()} already exists — updated in
     *       place, exactly like {@link #update}.</li>
     *   <li>No match on {@code entity.key()}, but {@code entity} collides with an
     *       existing row on one of the adapter's own declared unique constraints
     *       (not the primary key) — that existing row is updated with
     *       {@code entity}'s other field values. <b>The existing row's key is
     *       retained; {@code entity}'s own key is discarded.</b> This is the case
     *       a caller that mints a fresh surrogate key on every call (e.g. a fresh
     *       {@code EntityId} per attempt) needs: the unique-constrained value is
     *       the entity's real identity for dedup purposes, not the caller-supplied
     *       key, so repeated calls converge on exactly one persisted row instead
     *       of tripping the constraint.</li>
     *   <li>Neither matches — inserted as new, exactly like {@link #insert}.</li>
     * </ol>
     * Never throws {@link com.naturalist.exception.PrimaryKeyConstraintException}
     * or {@link com.naturalist.exception.EntityNotFoundException} for either of
     * those reasons — that is the point of this method existing separately from
     * {@link #insert}/{@link #update}. This is repository-owned, not
     * command-layer-owned, because the adapter is where the entity's own unique
     * constraints are already declared and enforced (e.g.
     * {@code TestEntitySource#uniqueConstraints()} today; a real upsert/merge
     * query in a future RDBMS adapter).
     *
     * <p><b>Returns the entity that was actually persisted</b> — {@code entity}
     * itself in cases 1 and 3, but a different value in case 2: the one carrying
     * the retained key. A caller that built a dependent record referencing
     * {@code entity}'s own (possibly-discarded) key before calling {@code save}
     * — e.g. an {@code InsectFeatureAssignment} referencing an {@code InsectFeature}'s
     * id — must rebuild that reference from the returned value, not the original
     * argument, or it ends up pointing at a key that was never actually written.
     */
    ENTITY save(ENTITY entity);
}
