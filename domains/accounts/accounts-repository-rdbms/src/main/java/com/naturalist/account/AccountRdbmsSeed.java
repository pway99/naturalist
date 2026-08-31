package com.naturalist.account;

import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;
import java.util.Collection;

/** Public seed entrypoint: replays already-loaded accounts + verification tokens through this
 *  module's rdbms adapters' insert(), FK-ordered (accounts before tokens), committing once.
 *  Callers (the seeder app) load the JSON-backed TestEntitySources and pass the entities in —
 *  this module never depends on the test-fixture module itself. */
public final class AccountRdbmsSeed {

    private AccountRdbmsSeed() {}

    public static void seed(DataSource dataSource,
                             Collection<Account> accounts,
                             Collection<EmailVerificationToken> tokens) {
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(
                dataSource, AccountMapper.class, EmailVerificationTokenMapper.class);

        try (SqlSession session = factory.openSession(false)) {
            var accountRepo = new AccountEntityRepositoryRdbms(session.getMapper(AccountMapper.class));
            accounts.forEach(accountRepo::insert);

            var tokenRepo = new EmailVerificationTokenRepositoryRdbms(session.getMapper(EmailVerificationTokenMapper.class));
            tokens.forEach(tokenRepo::insert);

            session.commit();
        }
    }
}
