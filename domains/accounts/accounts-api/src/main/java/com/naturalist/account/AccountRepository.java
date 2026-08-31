package com.naturalist.account;

import com.naturalist.data.EntityRepository;

import java.util.Optional;

/**
 * Namespace for the accounts sub-context's write-side repositories (ADR-020). A
 * {@code class}, not an {@code interface}, so nested repository contracts stay
 * {@code protected} — hidden from foreign packages while permitting same-package adapter
 * implementations. Promoted from the earlier N=1 top-level interface when the verification
 * token joined the package. Non-instantiable. Mirrors {@code NaturalistRepository}.
 */
class AccountRepository {

    private AccountRepository() {
    }

    protected interface AccountEntityRepository extends EntityRepository<AccountName, Account> {

        /**
         * Resolves an account by its login {@code email} (a {@code @UniqueValue}, so at most
         * one match). The primary lookup for authentication.
         */
        Optional<Account> getByEmail(String email);
    }

    protected interface VerificationTokenRepository
            extends EntityRepository<EmailVerificationTokenId, EmailVerificationToken> {
    }
}
