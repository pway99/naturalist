package com.naturalist.secrets;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentSecretProviderTest {

    private static final SecretName SECRET = SecretName.of("example-secret");

    @Test
    void present_isResolvedFromTheUpperSnakeVariable() {
        var provider = new EnvironmentSecretProvider(Map.of("EXAMPLE_SECRET", "not-a-real-secret"));
        assertThat(provider.lookup(SECRET)).contains(Secret.of("not-a-real-secret"));
        assertThat(provider.require(SECRET).fingerprint()).isEqualTo("ed24bc0b");
    }

    @Test
    void absent_isEmpty() {
        assertThat(new EnvironmentSecretProvider(Map.of()).lookup(SECRET)).isEmpty();
    }

    @Test
    void blank_isTreatedAsAbsent() {
        assertThat(new EnvironmentSecretProvider(Map.of("EXAMPLE_SECRET", "  ")).lookup(SECRET)).isEmpty();
    }

    @Test
    void require_onAbsent_namesTheSecretAndTheSource() {
        assertThatThrownBy(() -> new EnvironmentSecretProvider(Map.of()).require(SECRET))
                .isInstanceOf(MissingSecretException.class)
                .hasMessage("secret 'example-secret' not found in source 'environment'")
                .satisfies(e -> {
                    assertThat(((MissingSecretException) e).name()).isEqualTo(SECRET);
                    assertThat(((MissingSecretException) e).source()).isEqualTo("environment");
                });
    }

    @Test
    void source_isEnvironment() {
        assertThat(new EnvironmentSecretProvider(Map.of()).source()).isEqualTo("environment");
    }
}
