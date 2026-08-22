package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate soil physical characteristics repository: the production-named adapter the
 * app wires, temporarily backed by {@link SoilPhysicalCharacteristicsEntityRepositoryMock}
 * until the SQL body lands
 * (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class SoilPhysicalCharacteristicsEntityRepositoryRdms extends SoilPhysicalCharacteristicsEntityRepositoryMock {
    SoilPhysicalCharacteristicsEntityRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
