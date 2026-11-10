package com.naturalist.console.secrets;

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.apache.commons.lang3.StringUtils;

/**
 * {@code naturalist.secrets.*}: which source supplies deployment secrets (ADR-028).
 * {@code source} is {@code environment} (the default) or {@code file}; {@code directory}
 * is required for {@code file}.
 */
@ConfigurationProperties("naturalist.secrets")
public record SecretsProperties(String source, String directory) {
    public SecretsProperties {
        source = StringUtils.isBlank(source) ? "environment" : source.strip().toLowerCase(Locale.ROOT);
    }
}
