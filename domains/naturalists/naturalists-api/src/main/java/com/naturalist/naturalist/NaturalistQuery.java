package com.naturalist.naturalist;

import com.naturalist.data.EntityQuery;
import com.naturalist.naturalist.NaturalistEntityCollections.NaturalistCollection;

/**
 * Read port for {@link Naturalist} entities. Identity at the port is the
 * {@link NaturalistName} (ADR-021).
 */
public interface NaturalistQuery
        extends EntityQuery<NaturalistName, Naturalist, NaturalistCollection> {
}
