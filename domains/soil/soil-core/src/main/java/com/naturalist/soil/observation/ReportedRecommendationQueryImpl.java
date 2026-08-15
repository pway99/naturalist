package com.naturalist.soil.observation;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;

import java.util.Set;

/**
 * Thin adapter for {@link ReportedRecommendationQuery}: observe, dispatch, delegate (ADR-010).
 */
@DomainService
class ReportedRecommendationQueryImpl
        extends AbstractEntityQuery<
        ReportedRecommendationId,
        ReportedRecommendation,
        ReportedRecommendationCollection,
        ReportedRecommendationRepository>
        implements ReportedRecommendationQuery {

    ReportedRecommendationQueryImpl(ReportedRecommendationRepository repository) {
        super(repository);
    }

    @Override
    public ReportedRecommendationCollection findByNameSet(Set<ReportedRecommendationId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return ReportedRecommendationCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public ReportedRecommendationCollection forLabAnalysisId(LabAnalysisId labAnalysisId) {
        observer().arguments("forLabAnalysisId", i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        return ReportedRecommendationCollection.of(repository().getByLabAnalysisId(labAnalysisId));
    }

    @Override
    public ReportedRecommendationCollection forInputName(RecommendedInputName inputName) {
        observer().arguments("forInputName", i -> i.entityName(inputName, "inputName"))
                .throwWhenInvalid();
        return ReportedRecommendationCollection.of(repository().getByInputName(inputName));
    }
}
