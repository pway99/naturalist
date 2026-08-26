package com.naturalist.usage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate usage counter repository: the production-named adapter the
 * app wires, temporarily backed by {@link UsageCounterRepositoryMock} until
 * the SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class UsageCounterRepositoryRdms extends UsageCounterRepositoryMock {
    UsageCounterRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
