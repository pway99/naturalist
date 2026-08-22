package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate plant genus repository: the production-named adapter the app
 * wires, temporarily backed by {@link PlantGenusRepositoryMock} until the SQL
 * body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class PlantGenusRepositoryRdms extends PlantGenusRepositoryMock {
    PlantGenusRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
