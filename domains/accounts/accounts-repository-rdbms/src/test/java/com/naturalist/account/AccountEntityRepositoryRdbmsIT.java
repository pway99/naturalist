package com.naturalist.account;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The behavioral contract ({@link AccountRepositoryTest}) re-run against real Postgres.
 * Requires the standing DB to be seeded (apps/test-db-seeder); each test runs in a
 * transaction the extension rolls back. `source()` (inherited) is the in-memory JSON oracle.
 */
class AccountEntityRepositoryRdbmsIT implements AccountRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public AccountRepository.AccountEntityRepository repository() {
        return new AccountEntityRepositoryRdbms(rdbms.mapper(AccountMapper.class));
    }
}
