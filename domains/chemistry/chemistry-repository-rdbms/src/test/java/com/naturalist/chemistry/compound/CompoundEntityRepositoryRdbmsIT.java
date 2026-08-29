package com.naturalist.chemistry.compound;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link CompoundEntityRepositoryTest} behavioral contract re-run against real Postgres —
 * exercises the aggregate assembly (parent + child tables) and the delete-and-reinsert update
 * path. Requires the seeded standing DB; each test rolls back.
 */
class CompoundEntityRepositoryRdbmsIT implements CompoundEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public CompoundRepository.CompoundEntityRepository repository() {
        return new CompoundEntityRepositoryRdbms(rdbms.mapper(CompoundMapper.class));
    }
}
