package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate insects functional role repository: the production-named
 * adapter the app wires, temporarily backed by
 * {@link InsectFunctionalRoleRepositoryMock} until the SQL body lands
 * (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class InsectFunctionalRoleRepositoryRdms extends InsectFunctionalRoleRepositoryMock {
    InsectFunctionalRoleRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
