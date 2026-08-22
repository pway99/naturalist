package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate plant species repository: the production-named adapter the app
 * wires, temporarily backed by {@link PlantSpeciesRepositoryMock} until the SQL
 * body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class PlantSpeciesRepositoryRdms extends PlantSpeciesRepositoryMock {
    PlantSpeciesRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
