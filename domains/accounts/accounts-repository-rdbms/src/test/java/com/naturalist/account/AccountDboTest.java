package com.naturalist.account;

import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountDboTest {

    private static Account sample(String slug, String email, boolean verified) {
        return new Account(AccountName.of(slug), email, "{bcrypt}$2a$10$abc", verified,
                AccessLevel.BROWSE_ONLY, AccountStatus.ACTIVE);
    }

    @Test void roundTrip_preservesAllFields() {
        Account a = sample("acct-018f3a2eabcd", "someone@example.org", true);
        assertThat(AccountDbo.from(a).toEntity()).usingRecursiveComparison().isEqualTo(a);
    }

    @Test void roundTrip_preservesUnverifiedAndSuspendedStates() {
        Account a = new Account(AccountName.of("acct-018f3a2eabce"), "other@example.org",
                "{bcrypt}$2a$10$abc", false, AccessLevel.VISION, AccountStatus.SUSPENDED);
        assertThat(AccountDbo.from(a).toEntity()).usingRecursiveComparison().isEqualTo(a);
    }

    @Test void invariants_flagOverLongName() {
        AccountDbo dbo = AccountDbo.from(sample("acct-018f3a2eabcd", "someone@example.org", true));
        dbo.name = "x".repeat(65); // exceeds VARCHAR(64)
        var violations = Observer.forClass(AccountDboTest.class)
                .arguments("t", i -> i.observable(dbo, "dbo")).violations();
        assertThat(violations).isNotEmpty();
    }

    @Test void from_throwsWhenAValueExceedsItsColumnWidth() {
        // passwordHash is unconstrained on the entity but the DBO/column caps it at 80 —
        // from(...) must fail here, one step before the database would truncate/reject.
        Account a = new Account(AccountName.of("acct-018f3a2eabcd"), "someone@example.org",
                "{bcrypt}" + "x".repeat(80), true, AccessLevel.BROWSE_ONLY, AccountStatus.ACTIVE);
        assertThatThrownBy(() -> AccountDbo.from(a))
                .isInstanceOf(InvariantViolationException.class);
    }
}
