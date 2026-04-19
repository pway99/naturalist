package com.naturalist.insects;

import java.util.Optional;

/**
 * Assembly contract for {@link InsectAggregate}. Implementations compose the aggregate
 * by looking up the {@link InsectSpecies} root by name and its associated
 * {@link InsectImage} children, then wrapping the result in an {@link InsectAggregate}.
 *
 * <p>The factory owns the referential integrity invariant: every returned aggregate's
 * {@code images} carry the root species's {@link InsectSpeciesName}, because images are
 * queried <i>by</i> that name. The aggregate's own {@code invariants()} enforces only
 * structural validity (presence of root, presence of collection); referential correctness
 * is tautological at the factory's construction point.
 */
interface InsectAggregateFactory {

    Optional<InsectAggregate> buildByName(InsectSpeciesName name);
}
