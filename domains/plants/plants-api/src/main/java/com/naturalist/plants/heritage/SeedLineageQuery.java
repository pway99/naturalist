package com.naturalist.plants.heritage;

import com.naturalist.data.EntityQuery;
import com.naturalist.plants.cultivar.CultivarName;

/**
 * Read surface for the heritage sub-context. N=1 collapse (ADR-020): the sub-context
 * holds a single entity, so this query <em>is</em> the entity query — no wrapping
 * namespace, no accessor. Mirrors the top-level convention insects uses.
 */
public interface SeedLineageQuery extends EntityQuery<SeedLineageName, SeedLineage, SeedLineageCollection> {

    SeedLineageCollection forCultivarName(CultivarName cultivarName);
}
