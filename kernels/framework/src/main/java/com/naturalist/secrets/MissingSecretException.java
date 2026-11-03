package com.naturalist.secrets;

/**
 * A required secret is absent from the live source (ADR-028). The message names
 * the secret and the source; it never carries a value.
 */
public class MissingSecretException extends RuntimeException {
    private final SecretName name;

    private final String source;

    public MissingSecretException(SecretName name, String source) {
        super("secret '" + name + "' not found in source '" + source + "'");
        this.name = name;
        this.source = source;
    }

    public SecretName name() {
        return name;
    }

    public String source() {
        return source;
    }
}
