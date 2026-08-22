package com.naturalist.plants.heritage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate plant seed lineage repository: the production-named adapter
 * the app wires, temporarily backed by {@link PlantSeedLineageRepositoryMock}
 * until the SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class PlantSeedLineageRepositoryRdms extends PlantSeedLineageRepositoryMock {
    PlantSeedLineageRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
