package com.naturalist.account;

import com.naturalist.data.EntityRepository;

/**
 * Write-side repository for {@link Account}. Package-private per ADR-020: adapters in the
 * same package (the in-memory mock, the future RDBMS adapter) implement it, while foreign
 * packages reach accounts only through the public query/command surface.
 *
 * <p>Collapsed to a top-level interface under the ADR-020 N=1 rule — {@code accounts} holds
 * a single entity today. Promote to a {@code AccountRepository} namespace {@code class} with
 * a nested {@code AccountEntityRepository} when a second entity (the verification token)
 * joins the package.
 */
interface AccountRepository extends EntityRepository<AccountName, Account> {
}
