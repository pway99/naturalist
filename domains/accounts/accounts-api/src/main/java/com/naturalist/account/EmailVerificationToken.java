package com.naturalist.account;

import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * A single-use token proving control of an email address — for verification at registration
 * or a password reset, per its {@link TokenPurpose}. Keyed by a surrogate
 * {@link EmailVerificationTokenId}; references its owning account by {@link AccountName}
 * (an intra-domain foreign key).
 *
 * <p>{@code tokenHash} is a hash of the raw token — the raw value lives only in the emailed
 * link, never at rest. {@code expiresAt} bounds validity; {@code consumedAt} is {@code null}
 * until the token is redeemed, enforcing single use.
 */
public record EmailVerificationToken(
        EmailVerificationTokenId id,
        AccountName account,
        String tokenHash,
        TokenPurpose purpose,
        Instant expiresAt,
        @Nullable Instant consumedAt
) implements Entity<EmailVerificationTokenId> {

    /** The redeemed copy — stamps {@code consumedAt} so the token cannot be reused. */
    public EmailVerificationToken withConsumedAt(Instant consumedAt) {
        return new EmailVerificationToken(id, account, tokenHash, purpose, expiresAt, consumedAt);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(account, "account")
                .notBlank(tokenHash, "tokenHash")
                .notNull(purpose, "purpose")
                .notNull(expiresAt, "expiresAt");
    }
}
