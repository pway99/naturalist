package com.naturalist.account;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.test.context.Fake;
import com.naturalist.test.context.Wired;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link AccountCommandImpl}, built by the test graph over the in-memory mocks
 * with production's {@link SecureTokens} and the policy {@link AccountsTestContextInternal} provides:
 * self-serve unless a test chooses another before the command is built, which is why the command comes
 * from {@code nte.get} and not a {@code @Wired} field. Time is a {@code @Fake Clock} the test moves
 * forward, so expiry is exercised without the wall clock. The seeded catalog ({@code accounts.json},
 * {@code email-verification-tokens.json}) supplies the duplicate-email case.
 */
class AccountCommandImplTest {

    private static final Instant MINT_TIME = Instant.parse("2026-09-01T10:00:00Z");

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Fake(Clock.class)
    MovingClock clock = new MovingClock(MINT_TIME);

    @Wired
    AccountRepository.AccountEntityRepository accounts;

    @Wired
    AccountRepository.VerificationTokenRepository tokens;

    @Wired
    SecureTokens secureTokens;

    /** The graph's command, built on first use under the policy chosen so far (self-serve by default). */
    private AccountCommand command() {
        return nte.get(AccountCommand.class);
    }

    /** The graph's command under {@code policy}; call before anything else has built the command. */
    private AccountCommand command(AccountPolicy policy) {
        nte.context(AccountsTestContextInternal.class).policy(policy);
        return command();
    }

    /** A fixed clock the test moves forward, so one command sees a token minted and later expired. */
    static final class MovingClock extends Clock {
        private Instant now;

        MovingClock(Instant start) {
            this.now = start;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void register_createsUnverifiedBrowseOnlyActiveAccount() {
        command().register("newbie@example.org", "{bcrypt}$2a$10$hash");

        Account account = accounts.getByEmail("newbie@example.org").orElseThrow();
        assertThat(account.emailVerified()).isFalse();
        assertThat(account.access()).isEqualTo(AccessLevel.BROWSE_ONLY);
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void register_returnsRawTokenAndPersistsOnlyItsHash() {
        AccountCommand.Registration reg = command().register("newbie@example.org", "hash");

        assertThat(reg.rawToken()).isNotBlank();
        assertThat(tokens.getByTokenHash(secureTokens.hash(reg.rawToken()))).isPresent();
    }

    @Test
    void register_rejectsDuplicateEmail() {
        assertThatThrownBy(() -> command().register("gerald.durrell@oakvista.example", "hash"))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void register_rejectsMalformedEmail() {
        assertThatThrownBy(() -> command().register("not-an-email", "hash"))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("email");
    }

    @Test
    void verifyEmail_selfServe_setsVerifiedAndGrantsVision() {
        AccountCommand cmd = command(AccountPolicy.selfServe());
        AccountCommand.Registration reg = cmd.register("verify-me@example.org", "hash");

        cmd.verifyEmail(reg.rawToken());

        Account account = accounts.getByEmail("verify-me@example.org").orElseThrow();
        assertThat(account.emailVerified()).isTrue();
        assertThat(account.access()).isEqualTo(AccessLevel.VISION);
    }

    @Test
    void verifyEmail_requestMode_verifiesButWithholdsVision() {
        AccountCommand cmd = command(AccountPolicy.request());
        AccountCommand.Registration reg = cmd.register("verify-me@example.org", "hash");

        cmd.verifyEmail(reg.rawToken());

        Account account = accounts.getByEmail("verify-me@example.org").orElseThrow();
        assertThat(account.emailVerified()).isTrue();
        assertThat(account.access()).isEqualTo(AccessLevel.BROWSE_ONLY);
    }

    @Test
    void verifyEmail_rejectsUnknownToken() {
        assertThatThrownBy(() -> command().verifyEmail("never-minted"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyEmail_rejectsAlreadyConsumedToken() {
        AccountCommand cmd = command();
        AccountCommand.Registration reg = cmd.register("once@example.org", "hash");
        cmd.verifyEmail(reg.rawToken());

        assertThatThrownBy(() -> cmd.verifyEmail(reg.rawToken()))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyEmail_rejectsExpiredToken() {
        AccountCommand cmd = command();
        AccountCommand.Registration reg = cmd.register("late@example.org", "hash");

        // token TTL is 24h from MINT_TIME; verify 25h later
        clock.advance(Duration.ofHours(25));
        assertThatThrownBy(() -> cmd.verifyEmail(reg.rawToken()))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyEmail_rejectsBlankToken() {
        assertThatThrownBy(() -> command().verifyEmail(""))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("rawToken");
    }

    @Test
    void grantVision_setsVisionAccess() {
        // acct-018f3a2ef533 is a seeded BROWSE_ONLY account
        command(AccountPolicy.request()).grantVision(AccountName.of("acct-018f3a2ef533"));

        assertThat(accounts.getByName(AccountName.of("acct-018f3a2ef533")).orElseThrow().access())
                .isEqualTo(AccessLevel.VISION);
    }

    @Test
    void grantVision_rejectsUnknownAccount() {
        assertThatThrownBy(() ->
                command(AccountPolicy.request()).grantVision(AccountName.of("acct-nobody")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void revokeVision_setsBrowseOnly() {
        // acct-018f3a2e9b71 is a seeded VISION account
        command(AccountPolicy.request()).revokeVision(AccountName.of("acct-018f3a2e9b71"));

        assertThat(accounts.getByName(AccountName.of("acct-018f3a2e9b71")).orElseThrow().access())
                .isEqualTo(AccessLevel.BROWSE_ONLY);
    }

    @Test
    void beginPasswordReset_existingEmail_returnsToken() {
        AccountCommand cmd = command();
        cmd.register("resetme@example.org", "old-hash");

        assertThat(cmd.beginPasswordReset("resetme@example.org")).isPresent();
    }

    @Test
    void beginPasswordReset_unknownEmail_returnsEmpty() {
        assertThat(command().beginPasswordReset("ghost@example.org")).isEmpty();
    }

    @Test
    void resetPassword_setsNewPasswordHash() {
        AccountCommand cmd = command();
        cmd.register("resetme@example.org", "old-hash");
        String rawToken = cmd.beginPasswordReset("resetme@example.org").orElseThrow();

        cmd.resetPassword(rawToken, "new-hash");

        assertThat(accounts.getByEmail("resetme@example.org").orElseThrow().passwordHash())
                .isEqualTo("new-hash");
    }

    @Test
    void resetPassword_rejectsVerificationToken() {
        AccountCommand cmd = command();
        AccountCommand.Registration reg = cmd.register("mixed@example.org", "hash");

        // reg.rawToken() is a VERIFY_EMAIL token — the wrong purpose for a reset
        assertThatThrownBy(() -> cmd.resetPassword(reg.rawToken(), "new-hash"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPassword_rejectsUnknownToken() {
        assertThatThrownBy(() -> command().resetPassword("never-minted", "new-hash"))
                .isInstanceOf(InvalidTokenException.class);
    }

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    @BeforeEach
    void bindMeters() {
        // A private registry on the global composite for the test's duration — the technique the
        // ADR-017 rationale documents — so each test reads only the metrics it caused.
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void unbindMeters() {
        Metrics.removeRegistry(meters);
    }

    private double violations(String decision) {
        // decision is "<method>.<predicate>"; the framework scopes it under the observing class.
        return checks("violated", decision);
    }

    private double baseline(String decision) {
        // ALWAYS mode: a decision that held is emitted as a held check — the baseline row.
        return checks("held", decision);
    }

    /**
     * The count of {@code AccountCommandImpl.<decision>} checks with {@code outcome}, under the production class's
     * name: the graph's traced subclass is synthetic, and {@code Observer} names a synthetic class for the class it
     * extends.
     */
    private double checks(String outcome, String decision) {
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", outcome)
                .tag("name", "AccountCommandImpl." + decision).counter();
        return counter == null ? 0 : counter.count();
    }

    @Test
    void register_duplicateEmail_isObservedAsEmailUnregisteredViolation() {
        assertThatThrownBy(() -> command().register("gerald.durrell@oakvista.example", "hash"))
                .isInstanceOf(DuplicateEmailException.class);

        assertThat(violations("register.emailUnregistered")).isEqualTo(1);
        assertThat(baseline("register.emailUnregistered")).isZero();
    }

    @Test
    void register_newEmail_isObservedAsBaseline() {
        command().register("newbie@example.org", "hash");

        assertThat(baseline("register.emailUnregistered")).isEqualTo(1);
        assertThat(violations("register.emailUnregistered")).isZero();
        // The written account is observed on the success path (ADR-017 producer rule).
        assertThat(baseline("register.created.email")).isEqualTo(1);
    }

    @Test
    void verifyEmail_eachInvalidTokenCause_isObservedUnderItsOwnName() {
        AccountCommand cmd = command();

        // unknown: never minted
        assertThatThrownBy(() -> cmd.verifyEmail("never-minted")).isInstanceOf(InvalidTokenException.class);
        assertThat(violations("verifyEmail.tokenKnown")).isEqualTo(1);

        // consumed: redeemed twice
        AccountCommand.Registration once = cmd.register("once@example.org", "hash");
        cmd.verifyEmail(once.rawToken());
        assertThatThrownBy(() -> cmd.verifyEmail(once.rawToken())).isInstanceOf(InvalidTokenException.class);
        assertThat(violations("verifyEmail.isConsumed")).isEqualTo(1);

        // wrong purpose: a verification token offered as a reset
        AccountCommand.Registration mixed = cmd.register("mixed@example.org", "hash");
        assertThatThrownBy(() -> cmd.resetPassword(mixed.rawToken(), "new-hash")).isInstanceOf(InvalidTokenException.class);
        assertThat(violations("resetPassword.hasPurpose")).isEqualTo(1);

        // expired: 25h after a 24h TTL
        AccountCommand.Registration late = cmd.register("late@example.org", "hash");
        clock.advance(Duration.ofHours(25));
        assertThatThrownBy(() -> cmd.verifyEmail(late.rawToken())).isInstanceOf(InvalidTokenException.class);
        assertThat(violations("verifyEmail.isExpired")).isEqualTo(1);

        // the message never distinguishes them; only the metric names do
        assertThat(violations("verifyEmail.hasPurpose")).isZero();
        assertThat(violations("resetPassword.tokenKnown")).isZero();
    }

    @Test
    void beginPasswordReset_unknownEmail_isObservedThoughTheResponseIsIdentical() {
        assertThat(command().beginPasswordReset("ghost@example.org")).isEmpty();

        assertThat(violations("beginPasswordReset.emailRegistered")).isEqualTo(1);
    }

    @Test
    void verifyEmail_suspendedAccount_isRejectedAndObserved() {
        AccountCommand cmd = command();
        AccountCommand.Registration reg = cmd.register("banned@example.org", "hash");
        Account account = accounts.getByEmail("banned@example.org").orElseThrow();
        accounts.update(account.withStatus(AccountStatus.SUSPENDED));

        assertThatThrownBy(() -> cmd.verifyEmail(reg.rawToken())).isInstanceOf(InvalidTokenException.class);

        assertThat(violations("verifyEmail.accountActive")).isEqualTo(1);
        assertThat(accounts.getByEmail("banned@example.org").orElseThrow().access()).isEqualTo(AccessLevel.BROWSE_ONLY);
    }

    @Test
    void suspend_andReinstate_flipStatusAndAreObserved() {
        AccountCommand cmd = command();
        cmd.register("troublemaker@example.org", "hash");
        AccountName name = accounts.getByEmail("troublemaker@example.org").orElseThrow().name();

        cmd.suspend(name);
        assertThat(accounts.getByName(name).orElseThrow().status()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(baseline("suspend.accountKnown")).isEqualTo(1);
        assertThat(baseline("suspend.suspended.status")).isEqualTo(1);

        cmd.reinstate(name);
        assertThat(accounts.getByName(name).orElseThrow().status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(baseline("reinstate.accountKnown")).isEqualTo(1);
    }

    @Test
    void suspend_unknownAccount_isRejectedAndObserved() {
        assertThatThrownBy(() -> command().suspend(AccountName.create()))
                .isInstanceOf(AccountNotFoundException.class);

        assertThat(violations("suspend.accountKnown")).isEqualTo(1);
    }
}
