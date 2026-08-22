package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate concept repository: the production-named adapter the app wires,
 * temporarily backed by {@link ConceptRepositoryMock} until the SQL body
 * lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class ConceptRepositoryRdms extends ConceptRepositoryMock {
    ConceptRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
