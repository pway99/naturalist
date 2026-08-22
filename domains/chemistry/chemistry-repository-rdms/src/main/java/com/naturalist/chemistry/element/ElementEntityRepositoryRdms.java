package com.naturalist.chemistry.element;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate element repository: the production-named adapter the app wires,
 * temporarily backed by {@link ElementEntityRepositoryMock} until the SQL body
 * lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class ElementEntityRepositoryRdms extends ElementEntityRepositoryMock {
    ElementEntityRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
