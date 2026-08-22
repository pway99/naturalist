package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate insects genus repository: the production-named adapter the
 * app wires, temporarily backed by {@link InsectGenusRepositoryMock} until
 * the SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class InsectGenusRepositoryRdms extends InsectGenusRepositoryMock {
    InsectGenusRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
