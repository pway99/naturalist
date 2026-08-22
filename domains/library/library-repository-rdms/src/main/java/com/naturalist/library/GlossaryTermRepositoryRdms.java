package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate glossary term repository: the production-named adapter the app wires,
 * temporarily backed by {@link GlossaryTermRepositoryMock} until the SQL body
 * lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class GlossaryTermRepositoryRdms extends GlossaryTermRepositoryMock {
    GlossaryTermRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
