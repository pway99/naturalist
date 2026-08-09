package com.naturalist.soil.observation;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.soil.SoilProfileName;

import java.util.List;

/**
 * In-memory {@link LabAnalysisInfoRepository} backed by {@link LabAnalysisInfoTestEntitySource}.
 */
@DomainService
class LabAnalysisInfoEntityRepositoryMock
        extends AbstractTestEntityRepository<LabAnalysisId, LabAnalysisInfo, LabAnalysisInfoTestEntitySource>
        implements LabAnalysisInfoRepository {

    protected LabAnalysisInfoEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<LabAnalysisInfo> getBySoilProfileName(SoilProfileName soilProfileName) {
        observer().arguments("getBySoilProfileName",
                        i -> i.entityName(soilProfileName, "soilProfileName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(analysis -> soilProfileName.equals(analysis.soilProfileName()))
                .toList();
    }
}
