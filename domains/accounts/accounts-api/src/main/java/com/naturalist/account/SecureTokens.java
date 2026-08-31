package com.naturalist.account;

/**
 * Mints and hashes single-use secrets for {@link EmailVerificationToken}s.
 *
 * <p>The raw token is high-entropy and handed back exactly once (for the emailed link); only
 * its <em>deterministic</em> hash is stored, so verification finds a presented token by
 * {@link #hash(String)}. A salted, slow password hash (bcrypt) is deliberately NOT used here —
 * a token must be locatable by its hash, which a per-value salt would prevent. The secret's
 * entropy, not hash slowness, is what protects it.
 *
 * <p>Injected into {@code AccountCommand} so tests can substitute a deterministic fake;
 * {@link #jdk()} is the production implementation.
 */
public interface SecureTokens {

    /** A freshly minted secret: the raw value to email, and the hash to persist. */
    record MintedToken(String rawValue, String hash) {}

    /** Generates a new high-entropy secret and its hash. */
    MintedToken mint();

    /** The deterministic hash of a presented raw token, for lookup. */
    String hash(String rawValue);

    /** Production implementation — {@code SecureRandom}(32 bytes) → base64url, SHA-256 → hex. */
    static SecureTokens jdk() {
        return JdkSecureTokens.INSTANCE;
    }
}
