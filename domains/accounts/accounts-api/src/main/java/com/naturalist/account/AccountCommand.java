package com.naturalist.account;

import java.util.Optional;

/**
 * Write port for accounts. Passwords arrive already-encoded (the app's {@code PasswordEncoder}) —
 * this port never sees a plaintext password. Raw verification / reset tokens are returned exactly
 * once for the caller to email; only their hashes are stored.
 */
public interface AccountCommand {

    /** The outcome of {@link #register}: the minted account handle, and the raw token to email. */
    record Registration(AccountName account, String rawToken) {}

    /**
     * Creates an unverified {@code BROWSE_ONLY}, {@code ACTIVE} account and mints its
     * email-verification token (24-hour TTL). Returns the minted {@link AccountName} and the raw
     * token for the emailed link. Throws {@link DuplicateEmailException} if the email is already
     * registered; {@code InvariantViolationException} if the email is malformed or the password
     * hash is blank.
     */
    Registration register(String email, String passwordHash);

    /**
     * Redeems an email-verification token: marks it consumed, sets {@code emailVerified}, and —
     * under a self-serve {@link AccountPolicy} — grants VISION. Throws {@link InvalidTokenException}
     * if the token is unknown, expired, already used, or of the wrong purpose.
     */
    void verifyEmail(String rawToken);

    /** Grants VISION access to an account (the request-mode admin path). */
    void grantVision(AccountName account);

    /** Revokes VISION access, returning the account to BROWSE_ONLY. */
    void revokeVision(AccountName account);

    /**
     * Mints a password-reset token if the email belongs to an account, returning the raw token to
     * email. Returns {@link Optional#empty()} when no account matches — the caller responds
     * identically either way, so the response cannot be used to enumerate registered emails.
     */
    Optional<String> beginPasswordReset(String email);

    /**
     * Redeems a password-reset token and sets the new (already-encoded) password hash. Throws
     * {@link InvalidTokenException} if the token is unknown, expired, already used, or not a
     * reset token.
     */
    void resetPassword(String rawToken, String newPasswordHash);
}
