package com.naturalist.insects;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link InsectOrderRepositoryTest} behavioral contract re-run against real Postgres — covers the
 * top-of-chain {@code insect_order} row, its four-column {@code Description}, the nullable
 * {@code placed_in} clade slug, and the {@code insect_order_common_name} child. Requires the seeded
 * standing DB; each test rolls back.
 */
class InsectOrderEntityRepositoryRdbmsIT implements InsectOrderRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public InsectRepository.OrderRepository repository() {
        return new InsectOrderEntityRepositoryRdbms(rdbms.mapper(InsectOrderMapper.class));
    }
}
