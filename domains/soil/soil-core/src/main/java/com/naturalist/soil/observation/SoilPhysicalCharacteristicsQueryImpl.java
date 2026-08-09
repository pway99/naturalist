package com.naturalist.soil.observation;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;

import java.util.Optional;
import java.util.Set;

/**
 * Thin adapter for {@link SoilPhysicalCharacteristicsQuery}: observe, dispatch, delegate (ADR-010).
 */
@DomainService
class SoilPhysicalCharacteristicsQueryImpl
        extends AbstractEntityQuery<
        SoilPhysicalCharacteristicsId,
        SoilPhysicalCharacteristics,
        SoilPhysicalCharacteristicsCollection,
        SoilPhysicalCharacteristicsRepository>
        implements SoilPhysicalCharacteristicsQuery {

    SoilPhysicalCharacteristicsQueryImpl(SoilPhysicalCharacteristicsRepository repository) {
        super(repository);
    }

    @Override
    public SoilPhysicalCharacteristicsCollection findByNameSet(Set<SoilPhysicalCharacteristicsId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return SoilPhysicalCharacteristicsCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public Optional<SoilPhysicalCharacteristics> forLabAnalysisId(LabAnalysisId labAnalysisId) {
        observer().arguments("forLabAnalysisId", i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        return repository().getByLabAnalysisId(labAnalysisId);
    }
}
