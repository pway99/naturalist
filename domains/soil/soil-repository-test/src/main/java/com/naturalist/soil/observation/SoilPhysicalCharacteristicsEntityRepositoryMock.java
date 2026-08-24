package com.naturalist.soil.observation;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory {@link SoilPhysicalCharacteristicsRepository} backed by
 * {@link SoilPhysicalCharacteristicsTestEntitySource}.
 */
class SoilPhysicalCharacteristicsEntityRepositoryMock
        extends AbstractTestEntityRepository<SoilPhysicalCharacteristicsId, SoilPhysicalCharacteristics, SoilPhysicalCharacteristicsTestEntitySource>
        implements SoilPhysicalCharacteristicsRepository {

    protected SoilPhysicalCharacteristicsEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Optional<SoilPhysicalCharacteristics> getByLabAnalysisId(LabAnalysisId labAnalysisId) {
        observer().arguments("getByLabAnalysisId",
                        i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(characteristics -> labAnalysisId.equals(characteristics.labAnalysisId()))
                .findFirst();
    }

    @Override
    public List<SoilPhysicalCharacteristics> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds) {
        observer().arguments("getByLabAnalysisIds",
                        i -> i.identifierSet(labAnalysisIds, "labAnalysisIds"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(characteristics -> labAnalysisIds.contains(characteristics.labAnalysisId()))
                .toList();
    }
}
