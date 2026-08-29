package com.naturalist.plants.phytochemistry;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link PlantPhytochemicalConstituentRepositoryTest} behavioral contract re-run against real
 * Postgres — covers the parent constituent row, its four-column {@link com.naturalist.fieldnotes.Description},
 * the roles / tissues child tables, and the {@code getByPlantName} / {@code getByCompoundName}
 * reverse lookups. Requires the seeded standing DB; each test rolls back.
 */
class PhytochemicalConstituentEntityRepositoryRdbmsIT implements PlantPhytochemicalConstituentRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public PhytochemicalConstituentRepository repository() {
        return new PhytochemicalConstituentEntityRepositoryRdbms(rdbms.mapper(PhytochemicalConstituentMapper.class));
    }
}
