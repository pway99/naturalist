package com.naturalist.account;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;
import com.naturalist.ddd.EntityName;

/**
 * Strongly typed, opaque natural key for {@code Account} entities.
 * <p>
 * The slug is a stable, non-self-identifying handle for an authentication
 * account (e.g. {@code AccountName.of("acct-018f3a2e9b71")}). It is minted by the
 * accounts domain and is the auth identity referenced across domain boundaries;
 * it carries no personal information and is never a login credential.
 */
public final class AccountName extends EntityName {

    private AccountName(String value) {
        super(value);
    }

    @JsonCreator
    public static AccountName of(String value) {
        return new AccountName(value);
    }

    /**
     * Mints a fresh opaque handle — {@code "acct-"} plus a UUIDv7 (hex, no hyphens), so it is
     * a valid kebab slug that carries no personal information. Uses the kernel generator;
     * never {@code UUID.randomUUID()}.
     */
    public static AccountName create() {
        return new AccountName("acct-" + EntityId.newUUID().toString().replace("-", ""));
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
