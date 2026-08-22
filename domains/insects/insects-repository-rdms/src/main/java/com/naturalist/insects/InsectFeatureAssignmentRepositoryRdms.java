package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate insects feature assignment repository: the production-named
 * adapter the app wires, temporarily backed by
 * {@link InsectFeatureAssignmentRepositoryMock} until the SQL body lands
 * (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class InsectFeatureAssignmentRepositoryRdms extends InsectFeatureAssignmentRepositoryMock {
    InsectFeatureAssignmentRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
