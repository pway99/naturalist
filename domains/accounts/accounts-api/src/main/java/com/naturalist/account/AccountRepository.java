package com.naturalist.account;

import com.naturalist.data.EntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

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

        /**
         * Registrations that lapsed unverified as of {@code now}, ordered by name: accounts whose
         * email is not verified, still {@code BROWSE_ONLY} and {@code ACTIVE}, that hold at least
         * one {@link TokenPurpose#VERIFY_EMAIL} token and no such token unexpired at {@code now}.
         * A verified account, one an admin granted VISION or suspended, and one whose
         * verification link is still live are never returned.
         */
        Page<Account> getLapsedRegistrations(Instant now, PageRequest pageRequest);

        /**
         * Deletes those of {@code names} that are still unverified, returning the names actually
         * deleted. A verified account is never deleted, whatever the caller passes. The accounts'
         * tokens must be deleted first ({@link VerificationTokenRepository#deleteForUnverifiedAccounts}),
         * as their foreign key requires.
         */
        Set<AccountName> deleteUnverified(Set<AccountName> names);
    }

    protected interface VerificationTokenRepository
            extends EntityRepository<EmailVerificationTokenId, EmailVerificationToken> {

        /**
         * Resolves a token by its stored hash (the deterministic hash of a presented raw
         * token). At most one match — the verification/reset lookup.
         */
        Optional<EmailVerificationToken> getByTokenHash(String tokenHash);

        /**
         * Deletes every token, of any purpose, owned by one of {@code accounts} whose email is
         * still unverified, returning how many were deleted. Tokens of a verified account are
         * never deleted. The child half of purging a lapsed registration.
         */
        int deleteForUnverifiedAccounts(Set<AccountName> accounts);
    }
}
