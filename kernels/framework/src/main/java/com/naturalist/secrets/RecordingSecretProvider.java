package com.naturalist.secrets;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps the live source and records the first successful resolution of each
 * secret (ADR-028, "Rotation is a rehearsed procedure"). {@link #resolutions()}
 * feeds {@code /admin/secrets}. Nothing is logged, and no value is ever recorded:
 * a resolution carries the name, the source and the fingerprint only.
 */
public final class RecordingSecretProvider implements SecretProvider {

    private final SecretProvider delegate;

    private final Clock clock;

    private final Map<SecretName, SecretResolution> resolved = new ConcurrentHashMap<>();

    public RecordingSecretProvider(SecretProvider delegate, Clock clock) {
        this.delegate = delegate;
        this.clock = clock;
    }

    @Override
    public Optional<Secret> lookup(SecretName name) {
        Optional<Secret> secret = delegate.lookup(name);
        secret.ifPresent(s -> resolved.computeIfAbsent(name,
                n -> new SecretResolution(n, delegate.source(), s.fingerprint(), clock.instant())));
        return secret;
    }

    @Override
    public String source() {
        return delegate.source();
    }

    /** Every secret resolved so far, sorted by name. */
    public List<SecretResolution> resolutions() {
        return resolved.values().stream()
                .sorted(Comparator.comparing(r -> r.name().value()))
                .toList();
    }
}
