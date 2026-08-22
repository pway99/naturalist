package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate plant feature repository: the production-named adapter the app
 * wires, temporarily backed by {@link PlantFeatureRepositoryMock} until the SQL
 * body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class PlantFeatureRepositoryRdms extends PlantFeatureRepositoryMock {
    PlantFeatureRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
