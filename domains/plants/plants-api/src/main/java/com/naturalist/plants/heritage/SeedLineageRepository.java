package com.naturalist.plants.heritage;

import com.naturalist.data.EntityRepository;
import com.naturalist.plants.cultivar.CultivarName;

import java.util.List;

/**
 * Persistence port for the heritage sub-context. N=1 collapse (ADR-020): a top-level
 * package-private interface rather than a namespace class, since the sub-context
 * holds a single entity.
 */
interface SeedLineageRepository extends EntityRepository<SeedLineageName, SeedLineage> {

    List<SeedLineage> getByCultivarName(CultivarName cultivarName);
}
