package com.naturalist.secrets;

import java.util.Optional;

/**
 * Port for deployment secrets (ADR-028). The port lives in the kernel and each
 * source is an adapter, as with {@code com.naturalist.notification.EmailSender}:
 * {@link EnvironmentSecretProvider} and {@link FileSecretProvider} ship here with
 * no dependencies, and a managed-store adapter is added only if a host cannot
 * inject a secret as a variable or a file. The composition root selects the
 * source by the {@code naturalist.secrets.source} property.
 *
 * <p>Consumers resolve a secret once, at construction, never per request.
 */
public interface SecretProvider {
    /** The secret, or empty when this source does not hold it or holds it blank. */
    Optional<Secret> lookup(SecretName name);

    /** A short, value-free description of this source, for boot logs and refusals. */
    String source();

    /** The secret, or {@link MissingSecretException} naming the secret and this source. */
    default Secret require(SecretName name) {
        return lookup(name).orElseThrow(() -> new MissingSecretException(name, source()));
    }
}
