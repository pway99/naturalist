package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate plant feature assignment repository: the production-named
 * adapter the app wires, temporarily backed by
 * {@link PlantFeatureAssignmentRepositoryMock} until the SQL body lands
 * (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class PlantFeatureAssignmentRepositoryRdms extends PlantFeatureAssignmentRepositoryMock {
    PlantFeatureAssignmentRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
