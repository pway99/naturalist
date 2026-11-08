package com.naturalist.soil.observation;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;
import java.util.Set;

/**
 * In-memory {@link NutrientReadingRepository} backed by {@link NutrientReadingTestEntitySource}.
 */
@MockDomainService
class NutrientReadingEntityRepositoryMock
        extends AbstractTestEntityRepository<NutrientReadingId, NutrientReading, NutrientReadingTestEntitySource>
        implements NutrientReadingRepository {

    protected NutrientReadingEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<NutrientReading> getByLabAnalysisId(LabAnalysisId labAnalysisId) {
        MethodObserver getByLabAnalysisId = observer().forMethod("getByLabAnalysisId");
        getByLabAnalysisId.arguments(i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(reading -> labAnalysisId.equals(reading.labAnalysisId()))
                .toList();
        getByLabAnalysisId.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<NutrientReading> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds) {
        MethodObserver getByLabAnalysisIds = observer().forMethod("getByLabAnalysisIds");
        getByLabAnalysisIds.arguments(i -> i.identifierSet(labAnalysisIds, "labAnalysisIds"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(reading -> labAnalysisIds.contains(reading.labAnalysisId()))
                .toList();
        getByLabAnalysisIds.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<NutrientReading> getByNutrientName(NutrientName nutrientName) {
        MethodObserver getByNutrientName = observer().forMethod("getByNutrientName");
        getByNutrientName.arguments(i -> i.entityName(nutrientName, "nutrientName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(reading -> nutrientName.equals(reading.nutrientName()))
                .toList();
        getByNutrientName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
