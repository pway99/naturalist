package com.naturalist.chemistry.compound;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate compound depiction repository: the production-named adapter the
 * app wires, temporarily backed by {@link DepictionEntityRepositoryMock} until
 * the SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class DepictionEntityRepositoryRdms extends DepictionEntityRepositoryMock {
    DepictionEntityRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
