package com.naturalist.soil.observation;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;

import java.util.Set;

/**
 * Thin adapter for {@link NutrientReadingQuery}: observe, dispatch, delegate (ADR-010).
 */
@DomainService
class NutrientReadingQueryImpl
        extends AbstractEntityQuery<
        NutrientReadingId,
        NutrientReading,
        NutrientReadingCollection,
        NutrientReadingRepository>
        implements NutrientReadingQuery {

    NutrientReadingQueryImpl(NutrientReadingRepository repository) {
        super(repository);
    }

    @Override
    public NutrientReadingCollection findByNameSet(Set<NutrientReadingId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return NutrientReadingCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public NutrientReadingCollection forLabAnalysisId(LabAnalysisId labAnalysisId) {
        observer().arguments("forLabAnalysisId", i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        return NutrientReadingCollection.of(repository().getByLabAnalysisId(labAnalysisId));
    }

    @Override
    public NutrientReadingCollection forNutrientName(NutrientName nutrientName) {
        observer().arguments("forNutrientName", i -> i.entityName(nutrientName, "nutrientName"))
                .throwWhenInvalid();
        return NutrientReadingCollection.of(repository().getByNutrientName(nutrientName));
    }
}
