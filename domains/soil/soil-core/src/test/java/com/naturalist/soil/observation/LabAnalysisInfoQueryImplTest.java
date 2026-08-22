package com.naturalist.soil.observation;

import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.soil.TestSoilIdentifiers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LabAnalysisInfoQueryImplTest
        implements EntityQueryContractTest<LabAnalysisId, LabAnalysisInfo, LabAnalysisInfoCollection> {

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    LabAnalysisInfoQuery query = new LabAnalysisInfoQueryImpl(new LabAnalysisInfoEntityRepositoryMock(db));

    @Override
    public EntityQuery<LabAnalysisId, LabAnalysisInfo, LabAnalysisInfoCollection> query() {
        return query;
    }

    @Override
    public LabAnalysisId notFoundName() {
        return TestSoilIdentifiers.SoilProfiles.NotFound.labAnalysis;
    }

    @Override
    public List<LabAnalysisId> knownEntityNames() {
        return List.of(
                TestSoilIdentifiers.SoilProfiles.Box1.LabAnalyses.labAnalysis,
                TestSoilIdentifiers.SoilProfiles.Backyard.LabAnalyses.labAnalysis);
    }

    @Test
    void forSoilProfileName_rejectsNull() {
        assertThatThrownBy(() -> query.forSoilProfileName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("soilProfileName");
    }

    @Test
    void forSoilProfileName_returnsAnalysesForProfile() {
        LabAnalysisInfoCollection result =
                query.forSoilProfileName(TestSoilIdentifiers.SoilProfiles.Box1.name);

        assertThat(result.stream())
                .extracting(LabAnalysisInfo::id)
                .contains(TestSoilIdentifiers.SoilProfiles.Box1.LabAnalyses.labAnalysis);
    }
}
