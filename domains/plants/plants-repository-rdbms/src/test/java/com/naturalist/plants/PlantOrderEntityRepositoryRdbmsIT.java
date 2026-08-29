package com.naturalist.plants;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link PlantOrderRepositoryTest} behavioral contract re-run against real Postgres — covers
 * the top-of-chain {@code plant_order} row, its four-column {@code Description}, the nullable
 * {@code placed_in} clade slug, and the {@code plant_order_common_name} child. Requires the seeded
 * standing DB; each test rolls back.
 */
class PlantOrderEntityRepositoryRdbmsIT implements PlantOrderRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public PlantRepository.OrderRepository repository() {
        return new PlantOrderEntityRepositoryRdbms(rdbms.mapper(PlantOrderMapper.class));
    }
}
