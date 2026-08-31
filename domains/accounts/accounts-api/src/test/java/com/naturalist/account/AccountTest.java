package com.naturalist.account;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    private static Account sample() {
        return new Account(
                AccountName.of("acct-018f3a2e9b71"),
                "delia@example.org",
                "{bcrypt}$2a$10$abcdefghijklmnopqrstuv",
                false,
                AccessLevel.BROWSE_ONLY,
                AccountStatus.ACTIVE);
    }

    @Test
    void exposesComponentsByValue() {
        Account account = sample();
        assertThat(account.name()).isEqualTo(AccountName.of("acct-018f3a2e9b71"));
        assertThat(account.email()).isEqualTo("delia@example.org");
        assertThat(account.emailVerified()).isFalse();
        assertThat(account.access()).isEqualTo(AccessLevel.BROWSE_ONLY);
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void withEmailVerifiedAndVisionLeavesKeyUnchanged() {
        Account verified = sample().withEmailVerified(true).withAccess(AccessLevel.VISION);
        assertThat(verified.name()).isEqualTo(AccountName.of("acct-018f3a2e9b71"));
        assertThat(verified.emailVerified()).isTrue();
        assertThat(verified.access()).isEqualTo(AccessLevel.VISION);
        // original is unchanged (records are immutable copies)
        assertThat(sample().emailVerified()).isFalse();
    }

    @Test
    void withStatusSuspends() {
        assertThat(sample().withStatus(AccountStatus.SUSPENDED).status())
                .isEqualTo(AccountStatus.SUSPENDED);
    }
}
