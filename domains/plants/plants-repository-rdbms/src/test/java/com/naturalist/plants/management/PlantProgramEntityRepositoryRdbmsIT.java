package com.naturalist.plants.management;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link PlantProgramRepositoryTest} behavioral contract re-run against real Postgres —
 * exercises the polymorphic {@code (plant_rank, plant_name)} reference, the nullable
 * {@code program_constraint} column, and the reverse getByPlantName lookup. Requires the seeded
 * standing DB; each test rolls back.
 */
class PlantProgramEntityRepositoryRdbmsIT implements PlantProgramRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public PlantProgramRepository repository() {
        return new PlantProgramEntityRepositoryRdbms(rdbms.mapper(PlantProgramMapper.class));
    }
}
