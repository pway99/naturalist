package com.naturalist.console;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VisionPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EnableVisionProperties.class);

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void missingProvider_isRefused_namingTheProperty(String bad) {
        assertThatThrownBy(() -> new VisionProperties(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("naturalist.vision.provider")
                .hasMessageContaining("none")
                .hasMessageContaining("anthropic");
    }

    @Test
    void provider_isTrimmedAndCaseInsensitive() {
        assertThat(new VisionProperties(" Anthropic ").provider()).isEqualTo("anthropic");
        assertThat(new VisionProperties("NONE").provider()).isEqualTo("none");
    }

    @Test
    void unknownProvider_isRefusedNamingTheAllowedValues() {
        assertThatThrownBy(() -> new VisionProperties("other"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("other")
                .hasMessageContaining("none")
                .hasMessageContaining("anthropic");
    }

    @Test
    void contextFailsWhenProviderAbsent() {
        contextRunner.run(context -> assertThat(context).hasFailed()
                .getFailure()
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("naturalist.vision.provider"));
    }

    @Test
    void contextStartsWhenProviderIsNone() {
        contextRunner.withPropertyValues("naturalist.vision.provider=none")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(VisionProperties.class).provider()).isEqualTo("none");
                });
    }

    @Configuration
    @EnableConfigurationProperties(VisionProperties.class)
    static class EnableVisionProperties {
    }
}
