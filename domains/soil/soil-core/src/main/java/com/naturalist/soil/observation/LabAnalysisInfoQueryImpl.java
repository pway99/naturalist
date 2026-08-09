package com.naturalist.soil.observation;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.soil.SoilProfileName;

import java.util.Set;

/**
 * Thin adapter for {@link LabAnalysisInfoQuery}: observe, dispatch, delegate (ADR-010).
 */
@DomainService
class LabAnalysisInfoQueryImpl
        extends AbstractEntityQuery<
        LabAnalysisId,
    LabAnalysisInfo,
    LabAnalysisInfoCollection,
    LabAnalysisInfoRepository>
        implements LabAnalysisInfoQuery {

    LabAnalysisInfoQueryImpl(LabAnalysisInfoRepository repository) {
        super(repository);
    }

    @Override
    public LabAnalysisInfoCollection findByNameSet(Set<LabAnalysisId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return LabAnalysisInfoCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public LabAnalysisInfoCollection forSoilProfileName(SoilProfileName soilProfileName) {
        observer().arguments("forSoilProfileName", i -> i.entityName(soilProfileName, "soilProfileName"))
                .throwWhenInvalid();
        return LabAnalysisInfoCollection.of(repository().getBySoilProfileName(soilProfileName));
    }
}
