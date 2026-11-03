package com.naturalist.secrets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretTest {

    @Test
    void fingerprint_isFirstEightHexOfSha256() {
        // printf %s "not-a-real-secret" | shasum -a 256 | cut -c1-8
        assertThat(Secret.of("not-a-real-secret").fingerprint()).isEqualTo("ed24bc0b");
    }

    @Test
    void fingerprint_isStableAcrossInstances() {
        assertThat(Secret.of("not-a-real-secret").fingerprint()).isEqualTo(Secret.of("not-a-real-secret").fingerprint());
    }

    @Test
    void toString_redactsTheValue() {
        assertThat(Secret.of("not-a-real-secret")).hasToString("Secret[ed24bc0b]");
        assertThat(Secret.of("not-a-real-secret").toString()).doesNotContain("not-a-real");
    }

    @Test
    void equality_isByValue() {
        assertThat(Secret.of("a"))
                .isEqualTo(Secret.of("a"))
                .hasSameHashCodeAs(Secret.of("a"))
                .isNotEqualTo(Secret.of("b"));
    }

    @Test
    void null_isRefused() {
        assertThatThrownBy(() -> Secret.of(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blank_isRefused(String bad) {
        assertThatThrownBy(() -> Secret.of(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("secret value must not be blank");
    }
}
