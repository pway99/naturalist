package com.naturalist.soil;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link SoilProfileInfoEntityRepositoryTest} behavioral contract re-run against real Postgres —
 * covers the flat soil-profile row and its two zone-slug columns. Requires the seeded standing DB;
 * each test rolls back.
 */
class SoilProfileInfoEntityRepositoryRdbmsIT implements SoilProfileInfoEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public SoilProfileInfoRepository repository() {
        return new SoilProfileInfoEntityRepositoryRdbms(rdbms.mapper(SoilProfileInfoMapper.class));
    }
}
