package com.naturalist.secrets;

import java.time.Instant;

/**
 * One observed resolution of a secret: which source supplied it, its
 * fingerprint and when (ADR-028). It has no value component, so it cannot leak one.
 */
public record SecretResolution(SecretName name, String source, String fingerprint, Instant resolvedAt) {
}
