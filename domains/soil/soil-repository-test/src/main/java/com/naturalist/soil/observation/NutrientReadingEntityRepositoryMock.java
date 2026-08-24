package com.naturalist.soil.observation;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;
import java.util.Set;

/**
 * In-memory {@link NutrientReadingRepository} backed by {@link NutrientReadingTestEntitySource}.
 */
class NutrientReadingEntityRepositoryMock
        extends AbstractTestEntityRepository<NutrientReadingId, NutrientReading, NutrientReadingTestEntitySource>
        implements NutrientReadingRepository {

    protected NutrientReadingEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<NutrientReading> getByLabAnalysisId(LabAnalysisId labAnalysisId) {
        observer().arguments("getByLabAnalysisId",
                        i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(reading -> labAnalysisId.equals(reading.labAnalysisId()))
                .toList();
    }

    @Override
    public List<NutrientReading> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds) {
        observer().arguments("getByLabAnalysisIds",
                        i -> i.identifierSet(labAnalysisIds, "labAnalysisIds"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(reading -> labAnalysisIds.contains(reading.labAnalysisId()))
                .toList();
    }

    @Override
    public List<NutrientReading> getByNutrientName(NutrientName nutrientName) {
        observer().arguments("getByNutrientName",
                        i -> i.entityName(nutrientName, "nutrientName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(reading -> nutrientName.equals(reading.nutrientName()))
                .toList();
    }
}
