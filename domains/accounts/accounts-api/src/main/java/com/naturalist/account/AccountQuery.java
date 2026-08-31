package com.naturalist.account;

import com.naturalist.account.AccountEntityCollections.AccountCollection;
import com.naturalist.data.EntityQuery;

/**
 * Read port for {@link Account} entities. Identity at the port is the opaque
 * {@link AccountName}; {@code getByName} resolves an account by that handle (the link
 * the naturalists domain holds). Login-by-email lookup ({@code getByEmail}) is a
 * follow-up — it adds a domain-specific finder to {@link AccountRepository}.
 */
public interface AccountQuery
        extends EntityQuery<AccountName, Account, AccountCollection> {
}
