package com.naturalist.soil.observation;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory {@link SoilPhysicalCharacteristicsRepository} backed by
 * {@link SoilPhysicalCharacteristicsTestEntitySource}.
 */
@MockDomainService
class SoilPhysicalCharacteristicsEntityRepositoryMock
        extends AbstractTestEntityRepository<SoilPhysicalCharacteristicsId, SoilPhysicalCharacteristics, SoilPhysicalCharacteristicsTestEntitySource>
        implements SoilPhysicalCharacteristicsRepository {

    protected SoilPhysicalCharacteristicsEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Optional<SoilPhysicalCharacteristics> getByLabAnalysisId(LabAnalysisId labAnalysisId) {
        MethodObserver getByLabAnalysisId = observer().forMethod("getByLabAnalysisId");
        getByLabAnalysisId.arguments(i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(characteristics -> labAnalysisId.equals(characteristics.labAnalysisId()))
                .findFirst();
        getByLabAnalysisId.state(s -> s.namedEntityOrNull(result.orElse(null), "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<SoilPhysicalCharacteristics> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds) {
        MethodObserver getByLabAnalysisIds = observer().forMethod("getByLabAnalysisIds");
        getByLabAnalysisIds.arguments(i -> i.identifierSet(labAnalysisIds, "labAnalysisIds"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(characteristics -> labAnalysisIds.contains(characteristics.labAnalysisId()))
                .toList();
        getByLabAnalysisIds.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
