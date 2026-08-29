package com.naturalist.plants;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link PlantFeatureAssignmentEntityRepositoryTest} behavioral contract re-run against real
 * Postgres — covers the surrogate-UUID key, the direct {@code feature_id} FK to {@code plant_feature},
 * and the batched {@code getByRankNames} row-value {@code IN}. Requires the seeded standing DB; each
 * test rolls back.
 */
class PlantFeatureAssignmentEntityRepositoryRdbmsIT implements PlantFeatureAssignmentEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public PlantRepository.FeatureAssignmentRepository repository() {
        return new PlantFeatureAssignmentEntityRepositoryRdbms(rdbms.mapper(PlantFeatureAssignmentMapper.class));
    }
}
