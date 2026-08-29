package com.naturalist.soil.observation;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link SoilPhysicalCharacteristicsEntityRepositoryTest} behavioral contract re-run against real
 * Postgres — covers the surrogate-UUID key, the soft 1:1 {@code labAnalysisId}, the richly-typed
 * measurement columns, the flattened {@link CationBaseSaturation}, and the 1:1 {@code getByLabAnalysisId}
 * lookup plus its batched sibling. Requires the seeded standing DB; each test rolls back.
 */
class SoilPhysicalCharacteristicsEntityRepositoryRdbmsIT implements SoilPhysicalCharacteristicsEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public SoilPhysicalCharacteristicsRepository repository() {
        return new SoilPhysicalCharacteristicsEntityRepositoryRdbms(
                rdbms.mapper(SoilPhysicalCharacteristicsMapper.class));
    }
}
