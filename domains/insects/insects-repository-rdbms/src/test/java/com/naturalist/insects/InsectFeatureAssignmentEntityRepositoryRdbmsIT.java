package com.naturalist.insects;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link InsectFeatureAssignmentEntityRepositoryTest} behavioral contract re-run against real
 * Postgres — covers the surrogate-UUID key, the direct {@code feature_id} FK to {@code insect_feature},
 * and the batched {@code getByRankNames} row-value {@code IN}. Requires the seeded standing DB; each
 * test rolls back.
 */
class InsectFeatureAssignmentEntityRepositoryRdbmsIT implements InsectFeatureAssignmentEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public InsectRepository.FeatureAssignmentRepository repository() {
        return new InsectFeatureAssignmentEntityRepositoryRdbms(rdbms.mapper(InsectFeatureAssignmentMapper.class));
    }
}
