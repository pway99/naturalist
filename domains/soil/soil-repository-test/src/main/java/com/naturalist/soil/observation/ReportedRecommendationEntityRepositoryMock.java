package com.naturalist.soil.observation;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

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
        MethodObserver getByLabAnalysisId = observer().forMethod("getByLabAnalysisId");
        getByLabAnalysisId.arguments(i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(recommendation -> labAnalysisId.equals(recommendation.labAnalysisId()))
                .toList();
        getByLabAnalysisId.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<ReportedRecommendation> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds) {
        MethodObserver getByLabAnalysisIds = observer().forMethod("getByLabAnalysisIds");
        getByLabAnalysisIds.arguments(i -> i.identifierSet(labAnalysisIds, "labAnalysisIds"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(recommendation -> labAnalysisIds.contains(recommendation.labAnalysisId()))
                .toList();
        getByLabAnalysisIds.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<ReportedRecommendation> getByInputName(RecommendedInputName inputName) {
        MethodObserver getByInputName = observer().forMethod("getByInputName");
        getByInputName.arguments(i -> i.entityName(inputName, "inputName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(recommendation -> inputName.equals(recommendation.inputName()))
                .toList();
        getByInputName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
