package com.naturalist.soil.observation;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;
import java.util.Set;

/**
 * In-memory {@link ReportedOptimumRepository} backed by {@link ReportedOptimumTestEntitySource}.
 */
class ReportedOptimumEntityRepositoryMock
        extends AbstractTestEntityRepository<ReportedOptimumId, ReportedOptimum, ReportedOptimumTestEntitySource>
        implements ReportedOptimumRepository {

    protected ReportedOptimumEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<ReportedOptimum> getByLabAnalysisId(LabAnalysisId labAnalysisId) {
        observer().arguments("getByLabAnalysisId",
                        i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(optimum -> labAnalysisId.equals(optimum.labAnalysisId()))
                .toList();
    }

    @Override
    public List<ReportedOptimum> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds) {
        observer().arguments("getByLabAnalysisIds",
                        i -> i.identifierSet(labAnalysisIds, "labAnalysisIds"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(optimum -> labAnalysisIds.contains(optimum.labAnalysisId()))
                .toList();
    }

    @Override
    public List<ReportedOptimum> getByNutrientName(NutrientName nutrientName) {
        observer().arguments("getByNutrientName",
                        i -> i.entityName(nutrientName, "nutrientName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(optimum -> nutrientName.equals(optimum.nutrientName()))
                .toList();
    }
}
