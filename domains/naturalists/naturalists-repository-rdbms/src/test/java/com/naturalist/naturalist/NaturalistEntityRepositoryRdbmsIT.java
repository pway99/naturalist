package com.naturalist.naturalist;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The behavioral contract ({@link NaturalistEntityRepositoryTest}) re-run against real
 * Postgres. Requires the standing DB to be seeded (apps/test-db-seeder); each test runs in a
 * transaction the extension rolls back. `source()` (inherited) is the in-memory JSON oracle.
 */
class NaturalistEntityRepositoryRdbmsIT implements NaturalistEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public NaturalistRepository.NaturalistEntityRepository repository() {
        return new NaturalistEntityRepositoryRdbms(rdbms.mapper(NaturalistMapper.class));
    }
}
