package com.naturalist.chemistry.compound;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate compound repository: the production-named adapter the app wires,
 * temporarily backed by {@link CompoundEntityRepositoryMock} until the SQL body
 * lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class CompoundEntityRepositoryRdms extends CompoundEntityRepositoryMock {
    CompoundEntityRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
