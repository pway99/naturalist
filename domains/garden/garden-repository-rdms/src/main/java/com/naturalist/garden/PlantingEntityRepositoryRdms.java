package com.naturalist.garden;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate planting repository: the production-named adapter the app
 * wires, temporarily backed by {@link PlantingEntityRepositoryMock} until the
 * SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class PlantingEntityRepositoryRdms extends PlantingEntityRepositoryMock {
    PlantingEntityRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
