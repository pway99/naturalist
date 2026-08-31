package com.naturalist.naturalist;

import com.naturalist.account.AccountName;
import com.naturalist.data.EntityQuery;
import com.naturalist.naturalist.NaturalistEntityCollections.NaturalistCollection;

import java.util.Optional;

/**
 * Read port for {@link Naturalist} entities. Identity at the port is the
 * {@link NaturalistName} (ADR-021).
 */
public interface NaturalistQuery
        extends EntityQuery<NaturalistName, Naturalist, NaturalistCollection> {

    /**
     * Resolves the naturalist linked to an {@code account} — the reverse lookup
     * "given an account, find its naturalist".
     */
    Optional<Naturalist> byAccount(AccountName account);
}
