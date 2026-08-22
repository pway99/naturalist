package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate lab analysis info repository: the production-named adapter the app
 * wires, temporarily backed by {@link LabAnalysisInfoEntityRepositoryMock} until the
 * SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class LabAnalysisInfoEntityRepositoryRdms extends LabAnalysisInfoEntityRepositoryMock {
    LabAnalysisInfoEntityRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
