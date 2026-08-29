package com.naturalist.chemistry.compound;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link DepictionEntityRepositoryTest} behavioral contract re-run against real Postgres —
 * covers the surrogate-UUID key, the compound FK resolution (nested-select / JOIN), and the
 * getByCompoundName / getAllDepictedCompoundNames lookups. Requires the seeded standing DB.
 */
class DepictionEntityRepositoryRdbmsIT implements DepictionEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public CompoundRepository.DepictionRepository repository() {
        return new DepictionEntityRepositoryRdbms(rdbms.mapper(DepictionMapper.class));
    }
}
