package com.naturalist.plants.cultivar;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate plant cultivar repository: the production-named adapter the
 * app wires, temporarily backed by {@link PlantCultivarRepositoryMock} until
 * the SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class PlantCultivarRepositoryRdms extends PlantCultivarRepositoryMock {
    PlantCultivarRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
