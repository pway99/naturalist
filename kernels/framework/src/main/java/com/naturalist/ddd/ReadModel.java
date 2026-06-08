package com.naturalist.ddd;

import com.naturalist.observability.Observable;

/**
 * A read-side composition assembled from already-persisted parts — a projection,
 * not a transactional consistency boundary.
 * <p>
 * A {@code ReadModel} is built at read time from entities that live in their own
 * repositories (e.g. a rank record plus its photographs). It is <b>immutable</b>,
 * its identity is <b>optional</b> (a read model may have none), and it is
 * <b>never the unit of a write or transaction</b>.
 * <p>
 * Its {@link #invariants()} assert the <i>structural well-formedness of the
 * projection</i> — required parts non-null, monotonic-fill, agreement among the
 * foreign keys of the assembled parts — <b>not</b> cross-entity consistency that
 * this type owns and mutates.
 * <p>
 * Contrast with {@link Aggregate}: an aggregate is a consistency boundary that
 * owns its child entities and value objects and is mutated as a unit (e.g.
 * {@code Zone}, {@code SoilProfile}). When a type is assembled for reading and
 * owns nothing, it is a {@code ReadModel}, not an {@code Aggregate}.
 *
 * @see Aggregate
 */
public interface ReadModel extends Observable {
}
