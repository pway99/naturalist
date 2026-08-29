package com.naturalist.plants.cultivar;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link PlantCultivarRepositoryTest} behavioral contract re-run against real Postgres —
 * exercises the {@code plant_species} FK join, the enum columns, and the reverse getByPlantName
 * lookup. Requires the seeded standing DB; each test rolls back.
 */
class CultivarEntityRepositoryRdbmsIT implements PlantCultivarRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public CultivarRepository repository() {
        return new CultivarEntityRepositoryRdbms(rdbms.mapper(CultivarMapper.class));
    }
}
