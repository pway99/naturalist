package com.naturalist.soil.observation;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link ReportedOptimumEntityRepositoryTest} behavioral contract re-run against real Postgres —
 * covers the surrogate-UUID key, the flattened {@link OptimumRange} (shape discriminator + nullable
 * bounds, including the upper-bounded-keeps-its-shape case), the soft {@code labAnalysisId}, and the
 * three reverse lookups. Requires the seeded standing DB; each test rolls back.
 */
class ReportedOptimumEntityRepositoryRdbmsIT implements ReportedOptimumEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public ReportedOptimumRepository repository() {
        return new ReportedOptimumEntityRepositoryRdbms(rdbms.mapper(ReportedOptimumMapper.class));
    }
}
