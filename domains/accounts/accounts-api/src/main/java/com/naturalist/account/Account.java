package com.naturalist.account;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Authentication account — the pure-auth identity, keyed by the opaque
 * {@link AccountName}. Login is by {@code email} (decoupled from any public
 * naturalist handle); {@code passwordHash} is a bcrypt-encoded hash, never
 * plaintext. {@code access} is the vision entitlement; {@code status} is the
 * lifecycle/ban state. The accounts domain knows nothing about naturalists — the
 * {@code NaturalistName ↔ AccountName} link is maintained by the naturalists domain.
 */
public record Account(
        AccountName name,
        String email,
        String passwordHash,
        boolean emailVerified,
        AccessLevel access,
        AccountStatus status
) implements NamedEntity<AccountName> {

    public Account withEmailVerified(boolean emailVerified) {
        return new Account(name, email, passwordHash, emailVerified, access, status);
    }

    public Account withAccess(AccessLevel access) {
        return new Account(name, email, passwordHash, emailVerified, access, status);
    }

    public Account withPasswordHash(String passwordHash) {
        return new Account(name, email, passwordHash, emailVerified, access, status);
    }

    public Account withStatus(AccountStatus status) {
        return new Account(name, email, passwordHash, emailVerified, access, status);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .email(email, "email")
                .notBlank(passwordHash, "passwordHash")
                .notNull(access, "access")
                .notNull(status, "status");
    }
}
