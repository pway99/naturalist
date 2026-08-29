package com.naturalist.usage;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link UsageCounterRepositoryTest} behavioral contract re-run against real Postgres. Counters are
 * JSON-seeded into the standing DB (like every other domain), so this IT is the standard shape.
 */
class UsageCounterEntityRepositoryRdbmsIT implements UsageCounterRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public UsageRepository.CounterRepository repository() {
        return new UsageCounterEntityRepositoryRdbms(rdbms.mapper(UsageCounterMapper.class));
    }
}
