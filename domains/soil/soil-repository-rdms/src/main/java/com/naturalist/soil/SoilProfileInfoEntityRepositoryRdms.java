package com.naturalist.soil;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate soil profile info repository: the production-named adapter the app
 * wires, temporarily backed by {@link SoilProfileInfoEntityRepositoryMock} until the
 * SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class SoilProfileInfoEntityRepositoryRdms extends SoilProfileInfoEntityRepositoryMock {
    SoilProfileInfoEntityRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
