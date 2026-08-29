package com.naturalist.chemistry.element;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link ElementEntityRepositoryTest} behavioral contract re-run against real Postgres.
 * Requires the standing DB to be seeded (apps/test-db-seeder); each test rolls back. The
 * inherited {@code source()} is the in-memory JSON oracle.
 */
class ElementEntityRepositoryRdbmsIT implements ElementEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public ElementRepository repository() {
        return new ElementEntityRepositoryRdbms(rdbms.mapper(ElementMapper.class));
    }
}
