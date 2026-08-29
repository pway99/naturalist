package com.naturalist.soil.observation;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link LabAnalysisInfoEntityRepositoryTest} behavioral contract re-run against real Postgres —
 * covers the surrogate-UUID key, the soft {@code soilProfileName} slug, the nullable
 * {@code SamplingProtocol} group, the nullable {@code DepthInches}, and the {@code getBySoilProfileName}
 * reverse lookup. Requires the seeded standing DB; each test rolls back.
 */
class LabAnalysisInfoEntityRepositoryRdbmsIT implements LabAnalysisInfoEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public LabAnalysisInfoRepository repository() {
        return new LabAnalysisInfoEntityRepositoryRdbms(rdbms.mapper(LabAnalysisInfoMapper.class));
    }
}
