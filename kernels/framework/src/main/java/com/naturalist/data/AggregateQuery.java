package com.naturalist.data;

import com.naturalist.ddd.Aggregate;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.PersistenceId;

/**
 * Query port for an {@link Aggregate}. Parameterized on the identifier type of the
 * aggregate's <i>root entity</i> — an aggregate carries no identity of its own; its
 * identity is the identity of its root.
 *
 * <p>A namespace may host multiple aggregates rooted at the same entity (e.g. a
 * catalog-view aggregate and a field-notes aggregate, both rooted at the same
 * species). Each is its own {@code AggregateQuery} with its own consistency boundary
 * and its own assembly factory.
 *
 * <p>Implementation pattern: the adapter's {@link #getById(PersistenceId)} and
 * {@link #getByName(EntityName)} methods validate arguments and delegate the
 * composition work to a domain-local aggregate factory. Each implementation should be
 * thin — observe, dispatch, delegate.
 *
 * @param <ID>   the root entity's persistence identifier type
 * @param <NAME> the root entity's name type
 * @param <A>    the aggregate type
 */
public interface AggregateQuery<ID extends PersistenceId<?>, NAME extends EntityName<?>, A extends Aggregate>
        extends Query<ID, NAME, A> {
}
