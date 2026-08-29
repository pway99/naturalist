package com.naturalist.soil.observation;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link NutrientReadingEntityRepositoryTest} behavioral contract re-run against real Postgres —
 * covers the surrogate-UUID key, the soft {@code labAnalysisId} slug, the exact-scale {@code NUMERIC}
 * value, and the three reverse lookups (including the batched {@code getByLabAnalysisIds}). Requires
 * the seeded standing DB; each test rolls back.
 */
class NutrientReadingEntityRepositoryRdbmsIT implements NutrientReadingEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public NutrientReadingRepository repository() {
        return new NutrientReadingEntityRepositoryRdbms(rdbms.mapper(NutrientReadingMapper.class));
    }
}
