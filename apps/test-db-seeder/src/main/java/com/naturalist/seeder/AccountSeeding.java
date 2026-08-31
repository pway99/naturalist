package com.naturalist.seeder;

import com.naturalist.account.Account;
import com.naturalist.account.AccountRdbmsSeed;
import com.naturalist.account.AccountTestEntitySource;
import com.naturalist.account.EmailVerificationToken;
import com.naturalist.account.EmailVerificationTokenTestEntitySource;
import com.naturalist.data.NaturalistDatabase;

import javax.sql.DataSource;
import java.util.List;

/** Seeds the accounts domain: its schema, then accounts and their verification tokens. */
final class AccountSeeding {

    private AccountSeeding() {}

    static void seed(DataSource dataSource, NaturalistDatabase database) {
        Seeding.applySchema(dataSource, "schema/accounts.sql");

        List<Account> accounts =
                database.getNamed(AccountTestEntitySource.class).entityStream().toList();
        List<EmailVerificationToken> tokens =
                database.getNamed(EmailVerificationTokenTestEntitySource.class).entityStream().toList();

        AccountRdbmsSeed.seed(dataSource, accounts, tokens);
    }
}
