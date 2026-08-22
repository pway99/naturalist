package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate citation repository: the production-named adapter the app wires,
 * temporarily backed by {@link CitationRepositoryMock} until the SQL body
 * lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class CitationRepositoryRdms extends CitationRepositoryMock {
    CitationRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
