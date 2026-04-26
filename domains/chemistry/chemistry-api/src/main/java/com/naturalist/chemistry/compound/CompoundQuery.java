package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.compound.CompoundEntityCollections.CompoundCollection;
import com.naturalist.chemistry.compound.CompoundEntityCollections.DepictionCollection;
import com.naturalist.data.EntityQuery;
import com.naturalist.ddd.EntityNameSet;

import java.util.Optional;

/**
 * Namespace query for the compound sub-context — the single discoverable entry point
 * for reading compound catalog data.
 *
 * <p>Nested queries scope to a single entity each:
 * <ul>
 *   <li>{@link CompoundEntityQuery} — {@link Compound} entities.</li>
 *   <li>{@link DepictionQuery} — {@link CompoundDepiction} entities.</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * compoundQuery.compounds().getByName(compoundName);    // Compound
 * compoundQuery.depictions().getByCompoundName(name);   // CompoundDepiction
 * }</pre>
 */
public interface CompoundQuery {

    CompoundEntityQuery compounds();

    DepictionQuery depictions();

    interface CompoundEntityQuery extends EntityQuery<CompoundName, Compound, CompoundCollection> {

        EntityNameSet<CompoundName> allCompoundNames();
    }

    interface DepictionQuery extends EntityQuery<DepictionId, CompoundDepiction, DepictionCollection> {

        Optional<CompoundDepiction> getByCompoundName(CompoundName compoundName);

        EntityNameSet<CompoundName> allDepictedCompounds();
    }
}
