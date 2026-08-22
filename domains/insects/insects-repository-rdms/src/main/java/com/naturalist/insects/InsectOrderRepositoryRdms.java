package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate insects order repository: the production-named adapter the
 * app wires, temporarily backed by {@link InsectOrderRepositoryMock} until
 * the SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class InsectOrderRepositoryRdms extends InsectOrderRepositoryMock {
    InsectOrderRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
