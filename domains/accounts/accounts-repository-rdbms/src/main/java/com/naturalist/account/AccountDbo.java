package com.naturalist.account;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.util.function.Consumer;

@DboSchema(table = "account", primaryKey = "id", unique = {"name", "email"}, entity = Account.class)
final class AccountDbo implements Dbo {
    Long id;              // null before insert; DB identity fills it
    String name;
    String email;
    String passwordHash;
    boolean emailVerified;
    String access;
    String status;

    static AccountDbo from(Account a) {
        AccountDbo d = new AccountDbo();
        d.name = a.name().value();
        d.email = a.email();
        d.passwordHash = a.passwordHash();
        d.emailVerified = a.emailVerified();
        d.access = a.access().name();
        d.status = a.status().name();
        Observer.forClass(AccountDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    Account toEntity() {
        return new Account(
                AccountName.of(name),
                email,
                passwordHash,
                emailVerified,
                AccessLevel.valueOf(access),
                AccountStatus.valueOf(status));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notBlank(email, "email").maxLength(email, 255, "email")
                .notBlank(passwordHash, "passwordHash").maxLength(passwordHash, 80, "passwordHash")
                .notNull(access, "access").maxLength(access, 32, "access")
                .notNull(status, "status").maxLength(status, 32, "status");
    }
}
