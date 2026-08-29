package com.naturalist.plants;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link PlantImageRepositoryTest} behavioral contract re-run against real Postgres — covers the
 * surrogate-UUID key, the flat polymorphic parent-rank reference, the optional observation FK, and the
 * getByParentName lookup. Requires the seeded standing DB; each test rolls back.
 */
class PlantImageEntityRepositoryRdbmsIT implements PlantImageRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public PlantRepository.ImageRepository repository() {
        return new PlantImageEntityRepositoryRdbms(rdbms.mapper(PlantImageMapper.class));
    }
}
