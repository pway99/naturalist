package com.naturalist.account;

import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationTokenDboTest {

    private static EmailVerificationToken sample(Instant consumedAt) {
        return new EmailVerificationToken(
                EmailVerificationTokenId.create(),
                AccountName.of("acct-018f3a2eabcd"),
                "sha256:abc123",
                TokenPurpose.VERIFY_EMAIL,
                Instant.parse("2026-12-01T00:00:00Z"),
                consumedAt);
    }

    @Test void roundTrip_recoversAllFields() {
        EmailVerificationToken t = sample(null);
        EmailVerificationTokenDbo dbo = EmailVerificationTokenDbo.from(t);
        assertThat(dbo.toEntity()).usingRecursiveComparison().isEqualTo(t);
    }

    @Test void roundTrip_preservesConsumedAt() {
        EmailVerificationToken t = sample(Instant.parse("2026-11-15T00:00:00Z"));
        EmailVerificationTokenDbo dbo = EmailVerificationTokenDbo.from(t);
        assertThat(dbo.toEntity()).usingRecursiveComparison().isEqualTo(t);
    }

    @Test void invariants_flagOverLongTokenHash() {
        EmailVerificationTokenDbo dbo = EmailVerificationTokenDbo.from(sample(null));
        dbo.tokenHash = "x".repeat(129); // exceeds VARCHAR(128)
        var violations = Observer.forClass(EmailVerificationTokenDboTest.class)
                .arguments("t", i -> i.observable(dbo, "dbo")).violations();
        assertThat(violations).isNotEmpty();
    }
}
