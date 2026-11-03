package com.naturalist.console;

import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.Resilience;
import com.naturalist.secrets.MissingSecretException;
import com.naturalist.secrets.SecretProvider;
import com.naturalist.vision.NoOpVisionService;
import com.naturalist.vision.VisionService;
import com.naturalist.vision.anthropic.AnthropicVisionConfig;
import com.naturalist.vision.anthropic.AnthropicVisionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the vision adapter by {@code naturalist.vision.provider} (ADR-028), never by
 * whether an environment variable happens to be set. {@code none} wires the no-op and is
 * observed as a standing {@code provider} violation so it shows on the dashboard;
 * {@code anthropic} resolves its credential through the {@link SecretProvider} and refuses
 * boot, naming the property, the secret and the source, when the credential is absent.
 */
@Configuration
@EnableConfigurationProperties(VisionProperties.class)
class VisionConfiguration {

    private final Observer observer = Observer.forClass(VisionConfiguration.class, Observer.MonitoringMode.ALWAYS);

    @Bean
    VisionService visionService(VisionProperties properties, SecretProvider secrets, Resilience resilience) {
        boolean anthropic = "anthropic".equals(properties.provider());
        observer.forMethod("visionService").arguments(i -> i.isTrue(anthropic, "provider")).observe(Level.WARN);
        if (!anthropic) {
            return new NoOpVisionService();
        }
        try {
            return new AnthropicVisionService(AnthropicVisionConfig.defaults(), secrets, resilience);
        } catch (MissingSecretException e) {
            throw new IllegalStateException("naturalist.vision.provider=anthropic but secret '" + e.name()
                    + "' was not found in source '" + e.source()
                    + "': supply it, or set naturalist.vision.provider=none", e);
        }
    }
}
