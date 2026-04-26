package com.naturalist.chemistry.compound.depiction;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.EntityQuery;
import com.naturalist.ddd.EntityNameSet;

import java.util.Optional;

/**
 * Read-side query for {@link CompoundDepiction} — the chemistry console's entry point
 * for resolving structural depiction inputs by compound.
 * <p>
 * Although {@code CompoundDepiction} is identified internally by surrogate
 * {@link DepictionId}, consumers reason about depictions through their parent
 * {@link CompoundName}: the depiction is conceptually pinned to a compound and is
 * unique per compound. {@link #getByCompoundName(CompoundName)} is the primary access
 * method; {@link #allDepictedCompounds()} drives the "which compounds have depictions"
 * filter in catalog views.
 */
public interface DepictionQuery extends EntityQuery<DepictionId, CompoundDepiction, DepictionCollection> {

    Optional<CompoundDepiction> getByCompoundName(CompoundName compoundName);

    EntityNameSet<CompoundName> allDepictedCompounds();
}
