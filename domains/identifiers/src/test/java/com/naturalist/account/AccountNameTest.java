package com.naturalist.account;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountNameTest {

    @Test
    void opaqueSlugIsValid() {
        AccountName name = AccountName.of("acct-018f3a2e9b71");
        assertThat(name.isValid()).isTrue();
        assertThat(name.value()).isEqualTo("acct-018f3a2e9b71");
    }

    @Test
    void nonKebabIsNotValid() {
        assertThat(AccountName.of("Bad Name").isNotValid()).isTrue();
    }

    @Test
    void overMaxLengthIsNotValid() {
        String tooLong = "a".repeat(65);
        assertThat(AccountName.of(tooLong).isNotValid()).isTrue();
    }
}
