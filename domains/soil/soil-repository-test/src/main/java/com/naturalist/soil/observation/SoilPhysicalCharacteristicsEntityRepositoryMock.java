package com.naturalist.soil.observation;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.Optional;

/**
 * In-memory {@link SoilPhysicalCharacteristicsRepository} backed by
 * {@link SoilPhysicalCharacteristicsTestEntitySource}.
 */
@DomainService
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
}
