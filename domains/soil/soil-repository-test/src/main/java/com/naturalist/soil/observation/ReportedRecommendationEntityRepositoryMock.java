package com.naturalist.soil.observation;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;
import java.util.Set;

/**
 * In-memory {@link ReportedRecommendationRepository} backed by
 * {@link ReportedRecommendationTestEntitySource}.
 */
@MockDomainService
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
    public List<ReportedRecommendation> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds) {
        observer().arguments("getByLabAnalysisIds",
                        i -> i.identifierSet(labAnalysisIds, "labAnalysisIds"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(recommendation -> labAnalysisIds.contains(recommendation.labAnalysisId()))
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
