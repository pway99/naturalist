package com.naturalist.secrets;

import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import com.naturalist.observability.Observer;

/**
 * Reads a secret from the process environment (ADR-028): {@code example-secret}
 * reads {@code EXAMPLE_SECRET}. A blank variable is treated as absent.
 * {@link #fromProcess()} is the one place app code reads the environment.
 */
public final class EnvironmentSecretProvider implements SecretProvider {
    private final Map<String, String> environment;

    public EnvironmentSecretProvider(Map<String, String> environment) {
        OBSERVER.arguments("constructor", i -> i
                .notNull(environment, "environment"))
                .throwWhenInvalid();
        this.environment = Map.copyOf(environment);
    }

    public static EnvironmentSecretProvider fromProcess() {
        return new EnvironmentSecretProvider(System.getenv());
    }

    @Override
    public Optional<Secret> lookup(SecretName name) {
        OBSERVER.arguments("lookup", i -> i
                .notNull(name, "name"))
                .throwWhenInvalid();
        String value = environment.get(name.environmentVariable());
        return StringUtils.isBlank(value) ? Optional.empty() : Optional.of(Secret.of(value));
    }

    @Override
    public String source() {
        return "environment";
    }

    private static final Observer OBSERVER = Observer.forClass(EnvironmentSecretProvider.class);
}
