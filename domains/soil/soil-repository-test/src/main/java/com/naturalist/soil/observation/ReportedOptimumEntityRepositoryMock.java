package com.naturalist.soil.observation;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;
import java.util.Set;

/**
 * In-memory {@link ReportedOptimumRepository} backed by {@link ReportedOptimumTestEntitySource}.
 */
@MockDomainService
class ReportedOptimumEntityRepositoryMock
        extends AbstractTestEntityRepository<ReportedOptimumId, ReportedOptimum, ReportedOptimumTestEntitySource>
        implements ReportedOptimumRepository {

    protected ReportedOptimumEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<ReportedOptimum> getByLabAnalysisId(LabAnalysisId labAnalysisId) {
        MethodObserver getByLabAnalysisId = observer().forMethod("getByLabAnalysisId");
        getByLabAnalysisId.arguments(i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(optimum -> labAnalysisId.equals(optimum.labAnalysisId()))
                .toList();
        getByLabAnalysisId.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<ReportedOptimum> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds) {
        MethodObserver getByLabAnalysisIds = observer().forMethod("getByLabAnalysisIds");
        getByLabAnalysisIds.arguments(i -> i.identifierSet(labAnalysisIds, "labAnalysisIds"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(optimum -> labAnalysisIds.contains(optimum.labAnalysisId()))
                .toList();
        getByLabAnalysisIds.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<ReportedOptimum> getByNutrientName(NutrientName nutrientName) {
        MethodObserver getByNutrientName = observer().forMethod("getByNutrientName");
        getByNutrientName.arguments(i -> i.entityName(nutrientName, "nutrientName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(optimum -> nutrientName.equals(optimum.nutrientName()))
                .toList();
        getByNutrientName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
