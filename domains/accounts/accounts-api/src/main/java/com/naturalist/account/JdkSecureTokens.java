package com.naturalist.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * JDK implementation of {@link SecureTokens}: a 256-bit {@link SecureRandom} secret encoded
 * as url-safe base64, hashed with SHA-256 to hex. No third-party dependency — mirrors the way
 * the framework's UUIDv7 generator ships in-kernel rather than behind a vendor adapter.
 */
final class JdkSecureTokens implements SecureTokens {

    static final JdkSecureTokens INSTANCE = new JdkSecureTokens();

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private JdkSecureTokens() {
    }

    @Override
    public MintedToken mint() {
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        String rawValue = URL_ENCODER.encodeToString(secret);
        return new MintedToken(rawValue, hash(rawValue));
    }

    @Override
    public String hash(String rawValue) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable", e);
        }
    }
}
