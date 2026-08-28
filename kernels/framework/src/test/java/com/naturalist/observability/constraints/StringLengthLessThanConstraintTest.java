package com.naturalist.observability.constraints;

import org.junit.jupiter.api.Test;
import java.util.function.Function;
import static org.assertj.core.api.Assertions.assertThat;

class StringLengthLessThanConstraintTest {

    private static StringLengthLessThanConstraint<String> c(String v, int max) {
        return new StringLengthLessThanConstraint<>(v, Function.identity(), max, "field");
    }

    @Test void nullValue_isValid() { assertThat(c(null, 5).isValid()).isTrue(); }

    @Test void atLimit_isValid() { assertThat(c("abcde", 5).isValid()).isTrue(); }

    @Test void underLimit_isValid() { assertThat(c("abc", 5).isValid()).isTrue(); }

    @Test void overLimit_isInvalid() { assertThat(c("abcdef", 5).isValid()).isFalse(); }

    @Test void overLimit_errorMessageNamesTheLengths() {
        assertThat(c("abcdef", 5).errorMessage()).contains("6").contains("5");
    }

    @Test void constraintsBuilder_maxLength_flagsOverLimit() {
        var observer = com.naturalist.observability.Observer.forClass(StringLengthLessThanConstraintTest.class);
        assertThat(observer.arguments("t", i -> i.maxLength("abcdef", 5, "field")).violations()).isNotEmpty();
        assertThat(observer.arguments("t", i -> i.maxLength("abc", 5, "field")).violations()).isEmpty();
    }
}
