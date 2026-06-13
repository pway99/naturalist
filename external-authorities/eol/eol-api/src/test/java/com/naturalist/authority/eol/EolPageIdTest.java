package com.naturalist.authority.eol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EolPageIdTest {

    @Test
    void nonBlankValueIsValid() {
        assertThat(EolPageId.of("1188585").isValid()).isTrue();
    }

    @Test
    void nullValueIsNotValid() {
        assertThat(new EolPageId(null).isValid()).isFalse();
    }

    @Test
    void blankValueIsNotValid() {
        assertThat(new EolPageId("  ").isValid()).isFalse();
    }
}