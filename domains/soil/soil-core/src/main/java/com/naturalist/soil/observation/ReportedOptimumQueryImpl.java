package com.naturalist.soil.observation;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;

import java.util.Set;

/**
 * Thin adapter for {@link ReportedOptimumQuery}: observe, dispatch, delegate (ADR-010).
 */
@DomainService
class ReportedOptimumQueryImpl
        extends AbstractEntityQuery<
        ReportedOptimumId,
        ReportedOptimum,
        ReportedOptimumCollection,
        ReportedOptimumRepository>
        implements ReportedOptimumQuery {

    ReportedOptimumQueryImpl(ReportedOptimumRepository repository) {
        super(repository);
    }

    @Override
    public ReportedOptimumCollection findByNameSet(Set<ReportedOptimumId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return ReportedOptimumCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public ReportedOptimumCollection forLabAnalysisId(LabAnalysisId labAnalysisId) {
        observer().arguments("forLabAnalysisId", i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        return ReportedOptimumCollection.of(repository().getByLabAnalysisId(labAnalysisId));
    }

    @Override
    public ReportedOptimumCollection forNutrientName(NutrientName nutrientName) {
        observer().arguments("forNutrientName", i -> i.entityName(nutrientName, "nutrientName"))
                .throwWhenInvalid();
        return ReportedOptimumCollection.of(repository().getByNutrientName(nutrientName));
    }
}
