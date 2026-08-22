package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate reported recommendation repository: the production-named adapter the app
 * wires, temporarily backed by {@link ReportedRecommendationEntityRepositoryMock} until
 * the SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class ReportedRecommendationEntityRepositoryRdms extends ReportedRecommendationEntityRepositoryMock {
    ReportedRecommendationEntityRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
