package com.naturalist.data;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.PersistenceId;
import com.naturalist.observability.Observer;

import java.util.Objects;
import java.util.Optional;

/**
 * Framework base class for all query adapter implementations in {@code <domain>-core}.
 *
 * <p>Per ADR-010, each domain defines a public query port in {@code <domain>-api} and a
 * single adapter implementation in {@code <domain>-core}. This class is the structural
 * contract every adapter shares: validated access to a repository, a scoped
 * {@link Observer}, and default implementations of the standard name- and id-based
 * single-entity lookups.
 *
 * <p><b>Argument validation.</b> Every public method validates its arguments via
 * {@link #observer()}.{@code arguments(...)} before touching the repository. Null and
 * structurally invalid arguments are programming errors — the observer collects all
 * violations in a single pass and throws {@code InvariantViolationException}. A valid argument
 * that produces no result (unknown name, unknown id) returns {@code Optional.empty()} or an
 * empty list; it is not an error. This is the ADR-010 default: the caller decides what
 * absence means. A subclass method that must throw for a missing entity should document
 * that explicitly.
 *
 * <p><b>Layered observation.</b> This class validates at the query layer and then delegates
 * to the repository's own validated public methods — never to the raw {@code do*} hooks.
 * Both layers observe independently. A future refactor that removes validation at one layer
 * does not silently remove protection at the other. As the {@link Observer} gains metrics
 * and monitoring capability, each layer emitting its own observations gives full data-flow
 * visibility — the same state transition is recorded at the query boundary and again at the
 * repository boundary.
 *
 * <p><b>Multi-result methods return behavioral collections.</b> {@link EntityQuery#findByNameSet}
 * and {@link EntityQuery#findByIdSet} are declared on the {@link EntityQuery} interface with
 * return type {@code EC extends BehavioralCollection<E>}. Subclasses implement them directly,
 * wrapping repository results in the domain's concrete
 * {@link com.naturalist.ddd.BehavioralCollection}. The type parameter makes the wrapping
 * requirement structural — returning a raw {@code List<E>} at a public boundary is a compile
 * error (ADR-011).
 *
 * <p><b>Observer scope.</b> The {@link Observer} is created with {@code getClass()} so
 * that diagnostic messages name the concrete adapter, not this abstract base.
 *
 * <p><b>Usage pattern in a domain core module:</b>
 * <pre>{@code
 * // chemistry-core
 * class CompoundQueryAdapter
 *         extends AbstractEntityQuery<CompoundId, CompoundName, Compound, CompoundCollection>
 *         implements CompoundQuery {
 *
 *     CompoundQueryAdapter(EntityRepository<CompoundId, CompoundName, Compound> repository) {
 *         super(repository);
 *     }
 *
 *     @Override
 *     public CompoundCollection findByNameSet(Set<CompoundName> names) {
 *         observer().arguments("findByNameSet", i -> i.notNull(names, "names"))
 *                   .throwWhenInvalid();
 *         return CompoundCollection.of(repository().getByEntityNameSet(names));
 *     }
 *
 *     @Override
 *     public CompoundCollection findByIdSet(Set<CompoundId> ids) {
 *         observer().arguments("findByIdSet", i -> i.notNull(ids, "ids"))
 *                   .throwWhenInvalid();
 *         return CompoundCollection.of(repository().getByIdSet(ids));
 *     }
 * }
 * }</pre>
 *
 * @param <ID>   the persistence identifier type for {@code E}
 * @param <NAME> the entity name type for {@code E}
 * @param <E>    the entity type this query operates over
 * @param <EC>   the behavioral collection type returned by multi-result methods
 *
 * @see com.naturalist.data.EntityRepository
 * @see com.naturalist.ddd.BehavioralCollection
 * @see com.naturalist.observability.Observer
 */
public abstract class AbstractEntityQuery<
        ID extends PersistenceId<?>,
        NAME extends EntityName<?>,
        E extends Entity<ID, NAME>,
        EC extends BehavioralCollection<E>> implements EntityQuery<ID, NAME, E, EC> {

    private final EntityRepository<ID, NAME, E> repository;
    private final Observer observer;

    /**
     * Constructs a query backed by the supplied repository.
     * The {@link Observer} is scoped to the concrete subclass — {@code getClass()} at
     * construction time is the runtime type of the adapter, not this abstract class.
     *
     * @param repository the entity repository to delegate reads to; must not be null
     */
    protected AbstractEntityQuery(EntityRepository<ID, NAME, E> repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.observer = Observer.forClass(getClass());
    }

    // ---------------------------------------------------------------------------------
    // Infrastructure access — for use by subclass query adapters
    // ---------------------------------------------------------------------------------

    /**
     * The observer scoped to this query adapter class.
     * Use for argument validation in domain-specific query methods:
     * <pre>{@code
     * observer().arguments("myMethod", i -> i.notNull(this, __ -> arg, "arg"))
     *           .throwWhenInvalid();
     * }</pre>
     */
    protected  Observer observer() {
        return observer;
    }

    /**
     * The entity repository backing this query.
     * Available to subclasses for domain-specific lookup patterns not covered by the
     * standard methods below.
     */
    protected EntityRepository<ID, NAME, E> repository() {
        return repository;
    }

    // ---------------------------------------------------------------------------------
    // Standard single-entity lookups — public, validated
    // ---------------------------------------------------------------------------------

    /**
     * Returns the entity with the given natural key, or {@code Optional.empty()} if no
     * entity carries that name.
     *
     * <p>An unknown name is not an error — the caller decides what absence means (ADR-010).
     * Throwing for a not-found name is permitted only in a subclass override that
     * explicitly documents the throwing contract.
     *
     * @param name the entity's natural key; must not be null or invalid
     * @throws com.naturalist.exception.InvariantViolationException if {@code name} is null or invalid
     */
    @Override
    public Optional<E> getByName(NAME name) {
        observer.arguments("getByName", i -> i.entityName(name, "name"))
                .throwWhenInvalid();
        return repository.getByName(name);
    }

    /**
     * Returns the entity with the given persistence identifier, or {@code Optional.empty()}
     * if no entity carries that id.
     *
     * <p>An unknown id is not an error — the caller decides what absence means (ADR-010).
     *
     * @param id the entity's persistence identifier; must not be null or invalid
     * @throws com.naturalist.exception.InvariantViolationException if {@code id} is null or invalid
     */
    @Override
    public Optional<E> getById(ID id) {
        observer.arguments("getById", i -> i.entityId(id, "id"))
                .throwWhenInvalid();
        return repository.getById(id);
    }

}
