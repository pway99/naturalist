package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate plant observation repository: the production-named adapter the
 * app wires, temporarily backed by {@link PlantObservationRepositoryMock} until
 * the SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class PlantObservationRepositoryRdms extends PlantObservationRepositoryMock {
    PlantObservationRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
