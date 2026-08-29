package com.naturalist.insects;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link InsectFeatureEntityRepositoryTest} behavioral contract re-run against real Postgres —
 * covers the surrogate-UUID key and the single-column {@code value} unique constraint. Requires the
 * seeded standing DB; each test rolls back.
 */
class InsectFeatureEntityRepositoryRdbmsIT implements InsectFeatureEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public InsectRepository.FeatureRepository repository() {
        return new InsectFeatureEntityRepositoryRdbms(rdbms.mapper(InsectFeatureMapper.class));
    }
}
