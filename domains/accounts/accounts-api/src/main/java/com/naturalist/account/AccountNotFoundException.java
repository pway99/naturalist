package com.naturalist.account;

/**
 * Thrown when a command targets an {@link AccountName} that no account carries — e.g. an admin
 * grant/revoke against a stale handle.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(AccountName account) {
        super("account not found: " + account.value());
    }
}
