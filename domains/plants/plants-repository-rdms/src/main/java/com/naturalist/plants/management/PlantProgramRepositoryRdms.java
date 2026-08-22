package com.naturalist.plants.management;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate plant program repository: the production-named adapter the
 * app wires, temporarily backed by {@link PlantProgramRepositoryMock} until
 * the SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class PlantProgramRepositoryRdms extends PlantProgramRepositoryMock {
    PlantProgramRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
