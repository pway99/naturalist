package com.naturalist.soil.observation;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

/**
 * In-memory {@link ReportedRecommendationRepository} backed by
 * {@link ReportedRecommendationTestEntitySource}.
 */
@DomainService
class ReportedRecommendationEntityRepositoryMock
        extends AbstractTestEntityRepository<
        ReportedRecommendationId, ReportedRecommendation, ReportedRecommendationTestEntitySource>
        implements ReportedRecommendationRepository {

    protected ReportedRecommendationEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<ReportedRecommendation> getByLabAnalysisId(LabAnalysisId labAnalysisId) {
        observer().arguments("getByLabAnalysisId",
                        i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(recommendation -> labAnalysisId.equals(recommendation.labAnalysisId()))
                .toList();
    }

    @Override
    public List<ReportedRecommendation> getByInputName(RecommendedInputName inputName) {
        observer().arguments("getByInputName",
                        i -> i.entityName(inputName, "inputName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(recommendation -> inputName.equals(recommendation.inputName()))
                .toList();
    }
}
