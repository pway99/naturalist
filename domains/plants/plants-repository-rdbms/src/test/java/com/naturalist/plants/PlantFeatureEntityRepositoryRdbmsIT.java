package com.naturalist.plants;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link PlantFeatureRepositoryTest} behavioral contract re-run against real Postgres — covers
 * the surrogate-UUID key and the single-column {@code value} unique constraint. Requires the seeded
 * standing DB; each test rolls back.
 */
class PlantFeatureEntityRepositoryRdbmsIT implements PlantFeatureRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public PlantRepository.FeatureRepository repository() {
        return new PlantFeatureEntityRepositoryRdbms(rdbms.mapper(PlantFeatureMapper.class));
    }
}
