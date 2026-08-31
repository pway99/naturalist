package com.naturalist.account;

import com.naturalist.account.AccountEntityCollections.AccountCollection;
import com.naturalist.data.EntityQuery;

import java.util.Optional;

/**
 * Read port for {@link Account} entities. {@code getByName} resolves an account by its
 * opaque {@link AccountName} handle (the link the naturalists domain holds);
 * {@code getByEmail} resolves it by login address (the authentication lookup).
 */
public interface AccountQuery
        extends EntityQuery<AccountName, Account, AccountCollection> {

    /**
     * Resolves an account by its login {@code email} (unique, so at most one match).
     */
    Optional<Account> getByEmail(String email);
}
