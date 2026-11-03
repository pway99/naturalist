package com.naturalist.console;

import java.util.Map;

import com.naturalist.resilience.Resilience;
import com.naturalist.secrets.EnvironmentSecretProvider;
import com.naturalist.secrets.MissingSecretException;
import com.naturalist.secrets.SecretProvider;
import com.naturalist.vision.NoOpVisionService;
import com.naturalist.vision.anthropic.AnthropicVisionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The vision provider is chosen by property, never by what the environment happens to hold,
 * and an unresolvable credential refuses boot. No Spring context.
 */
class VisionConfigurationTest {

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final VisionConfiguration configuration = new VisionConfiguration();

    @BeforeEach
    void setUp() {
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void none_wiresTheNoOp_andIsObservedAsAStandingViolation() {
        var service = configuration.visionService(new VisionProperties("none"), noSecrets(), Resilience.noOp());
        assertThat(service).isInstanceOf(NoOpVisionService.class);
        assertThat(violations("VisionConfiguration.visionService.provider")).isEqualTo(1);
    }

    @Test
    void none_ignoresACredentialThatIsPresent() {
        var service = configuration.visionService(new VisionProperties("none"), withCredential(), Resilience.noOp());
        assertThat(service).isInstanceOf(NoOpVisionService.class);
    }

    @Test
    void anthropic_withAResolvableCredential_wiresTheAdapter() {
        var service = configuration.visionService(new VisionProperties("anthropic"), withCredential(), Resilience.noOp());
        assertThat(service).isInstanceOf(AnthropicVisionService.class);
        assertThat(violations("VisionConfiguration.visionService.provider")).isZero();
    }

    @Test
    void anthropic_withNoCredential_refusesToBoot_namingPropertySecretAndSource() {
        assertThatThrownBy(() -> configuration.visionService(
                new VisionProperties("anthropic"), noSecrets(), Resilience.noOp()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("naturalist.vision.provider")
                .hasMessageContaining("model-credential")
                .hasMessageContaining("environment")
                .hasCauseInstanceOf(MissingSecretException.class);
    }

    private static SecretProvider noSecrets() {
        return new EnvironmentSecretProvider(Map.of());
    }

    private static SecretProvider withCredential() {
        return new EnvironmentSecretProvider(Map.of("MODEL_CREDENTIAL", "not-a-real-secret"));
    }

    private double violations(String name) {
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                .tag("name", name).counter();
        return counter == null ? 0 : counter.count();
    }
}
