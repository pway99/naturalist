package com.naturalist.soil.observation;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link ReportedRecommendationEntityRepositoryTest} behavioral contract re-run against real
 * Postgres — covers the surrogate-UUID key, the flattened {@link RecommendedAmount} (kind + nullable
 * value, keeping {@code None}, {@code BelowDetectionLimit}, and a zero {@code Quantity} distinct), the
 * nullable route, the soft {@code labAnalysisId}, and the three reverse lookups. Requires the seeded
 * standing DB; each test rolls back.
 */
class ReportedRecommendationEntityRepositoryRdbmsIT implements ReportedRecommendationEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public ReportedRecommendationRepository repository() {
        return new ReportedRecommendationEntityRepositoryRdbms(rdbms.mapper(ReportedRecommendationMapper.class));
    }
}
