package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate citation association repository: the production-named adapter the app
 * wires, temporarily backed by {@link CitationAssociationRepositoryMock} until the SQL
 * body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class CitationAssociationRepositoryRdms extends CitationAssociationRepositoryMock {
    CitationAssociationRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
