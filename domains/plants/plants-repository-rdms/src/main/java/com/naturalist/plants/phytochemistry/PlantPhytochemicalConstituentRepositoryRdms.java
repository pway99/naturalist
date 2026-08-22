package com.naturalist.plants.phytochemistry;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate plant phytochemical constituent repository: the
 * production-named adapter the app wires, temporarily backed by
 * {@link PlantPhytochemicalConstituentRepositoryMock} until the SQL body
 * lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class PlantPhytochemicalConstituentRepositoryRdms extends PlantPhytochemicalConstituentRepositoryMock {
    PlantPhytochemicalConstituentRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
