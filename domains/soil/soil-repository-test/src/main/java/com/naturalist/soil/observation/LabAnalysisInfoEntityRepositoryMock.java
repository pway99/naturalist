package com.naturalist.soil.observation;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.soil.SoilProfileName;

import java.util.List;

/**
 * In-memory {@link LabAnalysisInfoRepository} backed by {@link LabAnalysisInfoTestEntitySource}.
 */
@MockDomainService
class LabAnalysisInfoEntityRepositoryMock
        extends AbstractTestEntityRepository<LabAnalysisId, LabAnalysisInfo, LabAnalysisInfoTestEntitySource>
        implements LabAnalysisInfoRepository {

    protected LabAnalysisInfoEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<LabAnalysisInfo> getBySoilProfileName(SoilProfileName soilProfileName) {
        MethodObserver getBySoilProfileName = observer().forMethod("getBySoilProfileName");
        getBySoilProfileName.arguments(i -> i.entityName(soilProfileName, "soilProfileName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(analysis -> soilProfileName.equals(analysis.soilProfileName()))
                .toList();
        getBySoilProfileName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
