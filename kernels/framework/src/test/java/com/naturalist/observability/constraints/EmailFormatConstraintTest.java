package com.naturalist.observability.constraints;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class EmailFormatConstraintTest {

    private static EmailFormatConstraint<String> c(String v) {
        return new EmailFormatConstraint<>(v, Function.identity(), "email");
    }

    @Test void plainAddress_isValid() { assertThat(c("delia@example.org").isValid()).isTrue(); }

    @Test void subdomainAddress_isValid() { assertThat(c("a.b@mail.example.co.uk").isValid()).isTrue(); }

    @Test void missingAtSign_isInvalid() { assertThat(c("delia.example.org").isValid()).isFalse(); }

    @Test void missingDomainDot_isInvalid() { assertThat(c("delia@example").isValid()).isFalse(); }

    @Test void containsWhitespace_isInvalid() { assertThat(c("de lia@example.org").isValid()).isFalse(); }

    @Test void doubleAtSign_isInvalid() { assertThat(c("a@b@example.org").isValid()).isFalse(); }

    @Test void empty_isInvalid() { assertThat(c("").isValid()).isFalse(); }

    @Test void nullValue_isInvalid() { assertThat(c(null).isValid()).isFalse(); }

    @Test void overMaxLength_isInvalid() {
        String tooLong = "a".repeat(250) + "@b.co"; // 255 chars, well-formed but too long
        assertThat(c(tooLong).isValid()).isFalse();
    }

    @Test void overMaxLength_errorMessageNamesTheLengths() {
        String tooLong = "a".repeat(250) + "@b.co";
        assertThat(c(tooLong).errorMessage()).contains("255").contains("254");
    }

    @Test void malformed_errorMessageSaysNotValidEmail() {
        assertThat(c("delia.example.org").errorMessage()).contains("not a valid email");
    }

    @Test void nullValue_errorMessageReportsNullCarrier() {
        // direct-value form (Function.identity): a null value IS a null carrier,
        // matching KebabFormatConstraint's diagnostic contract.
        assertThat(c(null).errorMessage()).isEqualTo("null carrier");
    }

    @Test void constraintsBuilder_email_flagsInvalid() {
        var observer = com.naturalist.observability.Observer.forClass(EmailFormatConstraintTest.class);
        assertThat(observer.arguments("t", i -> i.email("delia@example.org", "email")).violations()).isEmpty();
        assertThat(observer.arguments("t", i -> i.email("not-an-email", "email")).violations()).isNotEmpty();
    }
}
