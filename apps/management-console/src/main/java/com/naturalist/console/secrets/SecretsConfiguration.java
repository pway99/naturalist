package com.naturalist.console.secrets;

import com.naturalist.secrets.EnvironmentSecretProvider;
import com.naturalist.secrets.FileSecretProvider;
import com.naturalist.secrets.SecretProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import org.apache.commons.lang3.StringUtils;

/**
 * Selects the deployment-secret source by {@code naturalist.secrets.source} (ADR-028),
 * the way {@code EmailConfiguration} selects the mail transport. This is the single
 * place that knows the source; a managed-store adapter is added here as a third case
 * only when a host cannot inject secrets as variables or files.
 */
@Configuration
@EnableConfigurationProperties(SecretsProperties.class)
class SecretsConfiguration {
    @Bean
    SecretProvider secretProvider(SecretsProperties properties) {
        return switch (properties.source()) {
            case "environment" -> EnvironmentSecretProvider.fromProcess();
            case "file" -> {
                if (StringUtils.isBlank(properties.directory())) {
                    throw new IllegalStateException(
                            "naturalist.secrets.source=file requires naturalist.secrets.directory");
                }
                yield new FileSecretProvider(Path.of(properties.directory().strip()));
            }
            default -> throw new IllegalStateException(
                    "naturalist.secrets.source must be 'environment' or 'file' (was '" + properties.source() + "')");
        };
    }
}
