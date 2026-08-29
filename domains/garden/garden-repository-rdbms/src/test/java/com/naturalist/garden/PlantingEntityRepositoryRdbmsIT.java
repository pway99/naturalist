package com.naturalist.garden;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link PlantingEntityRepositoryTest} behavioral contract re-run against real Postgres — covers the
 * surrogate-UUID key, the nullable polymorphic plant reference (matched by rank + slug), the zone/sub-zone
 * and cultivar slug columns, the LocalDate window, and the three reverse lookups. Requires the seeded
 * standing DB; each test rolls back.
 */
class PlantingEntityRepositoryRdbmsIT implements PlantingEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public PlantingRepository repository() {
        return new PlantingEntityRepositoryRdbms(rdbms.mapper(PlantingMapper.class));
    }
}
