package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate reported optimum repository: the production-named adapter the app
 * wires, temporarily backed by {@link ReportedOptimumEntityRepositoryMock} until the
 * SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class ReportedOptimumEntityRepositoryRdms extends ReportedOptimumEntityRepositoryMock {
    ReportedOptimumEntityRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
