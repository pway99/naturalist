package com.naturalist.naturalist;

import com.naturalist.account.AccountName;
import com.naturalist.data.EntityRepository;

import java.util.Optional;

/**
 * Namespace for the naturalists sub-context's write-side repositories.
 *
 * <p>A {@code class}, not an {@code interface}, so nested repository contracts stay
 * {@code protected} — hidden from foreign packages while permitting same-package
 * adapter implementations (ADR-020). Non-instantiable.
 */
class NaturalistRepository {

    private NaturalistRepository() {
    }

    protected interface NaturalistEntityRepository
            extends EntityRepository<NaturalistName, Naturalist> {

        /**
         * Resolves the naturalist linked to an {@code account} (not unique-constrained,
         * but at most one naturalist per account in practice — the reverse lookup
         * "given an account, find its naturalist").
         */
        Optional<Naturalist> getByAccount(AccountName account);
    }
}
