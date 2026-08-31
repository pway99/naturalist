package com.naturalist.account;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationTokenTest {

    private static EmailVerificationToken sample() {
        return new EmailVerificationToken(
                EmailVerificationTokenId.create(),
                AccountName.of("acct-018f3a2e9b71"),
                "sha256:abcdef0123456789abcdef0123456789",
                TokenPurpose.VERIFY_EMAIL,
                Instant.parse("2026-09-01T00:00:00Z"),
                null);
    }

    @Test
    void exposesComponentsByValue() {
        var token = sample();
        assertThat(token.account()).isEqualTo(AccountName.of("acct-018f3a2e9b71"));
        assertThat(token.purpose()).isEqualTo(TokenPurpose.VERIFY_EMAIL);
        assertThat(token.expiresAt()).isEqualTo(Instant.parse("2026-09-01T00:00:00Z"));
        assertThat(token.consumedAt()).isNull();
    }

    @Test
    void withConsumedAt_marksRedeemedAndLeavesIdentityUnchanged() {
        var token = sample();
        var consumedAt = Instant.parse("2026-08-31T12:00:00Z");

        var redeemed = token.withConsumedAt(consumedAt);

        assertThat(redeemed.consumedAt()).isEqualTo(consumedAt);
        assertThat(redeemed.id()).isEqualTo(token.id());
        assertThat(token.consumedAt()).isNull();
    }
}
