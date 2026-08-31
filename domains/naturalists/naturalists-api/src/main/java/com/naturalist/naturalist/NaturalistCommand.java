package com.naturalist.naturalist;

import com.naturalist.account.AccountName;
import org.jspecify.annotations.Nullable;

/**
 * Write port for naturalists. {@code create} mints a fresh, opaque {@link NaturalistName}
 * and links the new ecological record to an already-existing authentication {@link
 * AccountName} — the naturalists domain never creates or validates accounts itself.
 * {@code rename} changes only {@code publicHandle}, the one renameable field; the
 * opaque {@link NaturalistName} key never changes.
 */
public interface NaturalistCommand {

    /**
     * Creates a new {@link Naturalist} linked to {@code account}, mints its opaque
     * {@link NaturalistName}, and returns the minted name. Throws
     * {@code InvariantViolationException} if any argument is invalid.
     */
    NaturalistName create(AccountName account,
                          String publicHandle,
                          String givenName,
                          @Nullable String familyName,
                          NaturalistRole role,
                          EcologicalStage stage,
                          @Nullable String notes);

    /**
     * Renames the naturalist's public handle. Throws {@code InvariantViolationException}
     * if either argument is invalid, or a not-found exception if {@code name} is unknown.
     */
    void rename(NaturalistName name, String newPublicHandle);
}
