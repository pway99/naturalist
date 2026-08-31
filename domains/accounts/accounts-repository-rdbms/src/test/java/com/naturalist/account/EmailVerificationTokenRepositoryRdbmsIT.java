package com.naturalist.account;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The behavioral contract ({@link EmailVerificationTokenRepositoryTest}) re-run against real
 * Postgres. Requires the standing DB to be seeded (apps/test-db-seeder); each test runs in a
 * transaction the extension rolls back. `source()` (inherited) is the in-memory JSON oracle.
 *
 * <p>The shared contract's default {@code newEntity()}/{@code ghostEntity()} reference the
 * seeded {@code acct-018f3a2e9b71} account, so no override is needed here — the {@code account}
 * FK resolves against the real {@code account} table the same way it does against the mock.
 */
class EmailVerificationTokenRepositoryRdbmsIT implements EmailVerificationTokenRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public AccountRepository.VerificationTokenRepository repository() {
        return new EmailVerificationTokenRepositoryRdbms(rdbms.mapper(EmailVerificationTokenMapper.class));
    }
}
