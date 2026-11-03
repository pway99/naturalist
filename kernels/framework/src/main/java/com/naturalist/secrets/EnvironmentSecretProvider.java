package com.naturalist.secrets;

import java.util.Map;
import java.util.Optional;

/**
 * Reads a secret from the process environment (ADR-028): {@code example-secret}
 * reads {@code EXAMPLE_SECRET}. A blank variable is treated as absent.
 * {@link #fromProcess()} is the one place app code reads the environment.
 */
public final class EnvironmentSecretProvider implements SecretProvider {
    private final Map<String, String> environment;

    public EnvironmentSecretProvider(Map<String, String> environment) {
        this.environment = Map.copyOf(environment);
    }

    public static EnvironmentSecretProvider fromProcess() {
        return new EnvironmentSecretProvider(System.getenv());
    }

    @Override
    public Optional<Secret> lookup(SecretName name) {
        String value = environment.get(name.environmentVariable());
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(Secret.of(value));
    }

    @Override
    public String source() {
        return "environment";
    }
}
