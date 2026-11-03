package com.naturalist.secrets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretNameTest {

    @Test
    void kebabCase_isAccepted() {
        assertThat(SecretName.of("example-secret").value()).isEqualTo("example-secret");
    }

    @Test
    void environmentVariable_isUpperSnake() {
        assertThat(SecretName.of("example-secret").environmentVariable()).isEqualTo("EXAMPLE_SECRET");
    }

    @Test
    void toString_isTheValue() {
        assertThat(SecretName.of("other-secret")).hasToString("other-secret");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Example-Secret", "example_secret", "-leading", "trailing-", "a--b"})
    void nonKebab_isRefused(String bad) {
        assertThatThrownBy(() -> SecretName.of(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kebab-case");
    }

    @Test
    void null_isRefused() {
        assertThatThrownBy(() -> SecretName.of(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
