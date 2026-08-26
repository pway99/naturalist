package com.naturalist.usage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate usage alert repository: the production-named adapter the
 * app wires, temporarily backed by {@link UsageAlertRepositoryMock} until
 * the SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class UsageAlertRepositoryRdms extends UsageAlertRepositoryMock {
    UsageAlertRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
