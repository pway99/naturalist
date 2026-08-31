package com.naturalist.account;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

/**
 * In-memory catalog of {@link Account} fixtures — the analog of the {@code account}
 * table (ADR-001). The canonical key {@link AccountName} is enforced automatically;
 * {@code email} is the login and is {@code @UniqueValue}, so a secondary unique
 * constraint enforces at most one account per address. Accounts reference no other
 * entity, so there are no foreign-key constraints.
 */
public class AccountTestEntitySource extends TestEntitySource<AccountName, Account> {

    public AccountTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("account/accounts.json");
    }

    @Override
    protected List<UniqueConstraint<Account>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "email";
                    }

                    @Override
                    public Function<Account, ?> valueFunction() {
                        return Account::email;
                    }
                });
    }
}
