package com.naturalist.console.secrets;

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code naturalist.secrets.*}: which source supplies deployment secrets (ADR-028).
 * {@code source} is {@code environment} (the default) or {@code file}; {@code directory}
 * is required for {@code file}.
 */
@ConfigurationProperties("naturalist.secrets")
public record SecretsProperties(String source, String directory) {
    public SecretsProperties {
        source = source == null || source.isBlank() ? "environment" : source.strip().toLowerCase(Locale.ROOT);
    }
}
