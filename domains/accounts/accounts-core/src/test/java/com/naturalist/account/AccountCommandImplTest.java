package com.naturalist.account;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link AccountCommandImpl}. Wires the command over the in-memory mocks
 * with a fixed {@link Clock} and a deterministic fake {@link SecureTokens} so expiry and token
 * redemption are exercised without wall-clock or real randomness. The seeded catalog
 * ({@code accounts.json}, {@code email-verification-tokens.json}) supplies the duplicate-email case.
 */
class AccountCommandImplTest {

    private static final Instant MINT_TIME = Instant.parse("2026-09-01T10:00:00Z");

    private final Clock clock = Clock.fixed(MINT_TIME, ZoneOffset.UTC);

    /** Deterministic fake: raw values are sequential, the hash is a stable transform of the raw. */
    private final SecureTokens fakeTokens = new SecureTokens() {
        private int counter = 0;

        @Override
        public MintedToken mint() {
            String raw = "raw-token-" + (++counter);
            return new MintedToken(raw, hash(raw));
        }

        @Override
        public String hash(String rawValue) {
            return "hash:" + rawValue;
        }
    };

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    private final AccountRepository.AccountEntityRepository accounts = new AccountRepositoryMock(nte);
    private final AccountRepository.VerificationTokenRepository tokens =
            new EmailVerificationTokenRepositoryMock(nte);

    private AccountCommand command(AccountPolicy policy) {
        return new AccountCommandImpl(accounts, tokens, fakeTokens, clock, policy);
    }

    private AccountCommand commandAt(AccountPolicy policy, Instant at) {
        return new AccountCommandImpl(
                accounts, tokens, fakeTokens, Clock.fixed(at, ZoneOffset.UTC), policy);
    }

    @Test
    void register_createsUnverifiedBrowseOnlyActiveAccount() {
        command(AccountPolicy.selfServe()).register("newbie@example.org", "{bcrypt}$2a$10$hash");

        Account account = accounts.getByEmail("newbie@example.org").orElseThrow();
        assertThat(account.emailVerified()).isFalse();
        assertThat(account.access()).isEqualTo(AccessLevel.BROWSE_ONLY);
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void register_returnsRawTokenAndPersistsOnlyItsHash() {
        AccountCommand.Registration reg =
                command(AccountPolicy.selfServe()).register("newbie@example.org", "hash");

        assertThat(reg.rawToken()).isNotBlank();
        assertThat(tokens.getByTokenHash(fakeTokens.hash(reg.rawToken()))).isPresent();
    }

    @Test
    void register_rejectsDuplicateEmail() {
        assertThatThrownBy(() ->
                command(AccountPolicy.selfServe()).register("gerald.durrell@oakvista.example", "hash"))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void register_rejectsMalformedEmail() {
        assertThatThrownBy(() -> command(AccountPolicy.selfServe()).register("not-an-email", "hash"))
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
        assertThatThrownBy(() -> command(AccountPolicy.selfServe()).verifyEmail("never-minted"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyEmail_rejectsAlreadyConsumedToken() {
        AccountCommand cmd = command(AccountPolicy.selfServe());
        AccountCommand.Registration reg = cmd.register("once@example.org", "hash");
        cmd.verifyEmail(reg.rawToken());

        assertThatThrownBy(() -> cmd.verifyEmail(reg.rawToken()))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyEmail_rejectsExpiredToken() {
        AccountCommand.Registration reg =
                command(AccountPolicy.selfServe()).register("late@example.org", "hash");

        // token TTL is 24h from MINT_TIME; verify 25h later
        AccountCommand later = commandAt(AccountPolicy.selfServe(), MINT_TIME.plusSeconds(25 * 3600));
        assertThatThrownBy(() -> later.verifyEmail(reg.rawToken()))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyEmail_rejectsBlankToken() {
        assertThatThrownBy(() -> command(AccountPolicy.selfServe()).verifyEmail(""))
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
        AccountCommand cmd = command(AccountPolicy.selfServe());
        cmd.register("resetme@example.org", "old-hash");

        assertThat(cmd.beginPasswordReset("resetme@example.org")).isPresent();
    }

    @Test
    void beginPasswordReset_unknownEmail_returnsEmpty() {
        assertThat(command(AccountPolicy.selfServe()).beginPasswordReset("ghost@example.org"))
                .isEmpty();
    }

    @Test
    void resetPassword_setsNewPasswordHash() {
        AccountCommand cmd = command(AccountPolicy.selfServe());
        cmd.register("resetme@example.org", "old-hash");
        String rawToken = cmd.beginPasswordReset("resetme@example.org").orElseThrow();

        cmd.resetPassword(rawToken, "new-hash");

        assertThat(accounts.getByEmail("resetme@example.org").orElseThrow().passwordHash())
                .isEqualTo("new-hash");
    }

    @Test
    void resetPassword_rejectsVerificationToken() {
        AccountCommand cmd = command(AccountPolicy.selfServe());
        AccountCommand.Registration reg = cmd.register("mixed@example.org", "hash");

        // reg.rawToken() is a VERIFY_EMAIL token — the wrong purpose for a reset
        assertThatThrownBy(() -> cmd.resetPassword(reg.rawToken(), "new-hash"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPassword_rejectsUnknownToken() {
        assertThatThrownBy(() ->
                command(AccountPolicy.selfServe()).resetPassword("never-minted", "new-hash"))
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
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                .tag("name", "AccountCommandImpl." + decision).counter();
        return counter == null ? 0 : counter.count();
    }

    private double baseline(String decision) {
        // ALWAYS mode: a decision that held is emitted as a held check — the baseline row.
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "held")
                .tag("name", "AccountCommandImpl." + decision).counter();
        return counter == null ? 0 : counter.count();
    }

    @Test
    void register_duplicateEmail_isObservedAsEmailUnregisteredViolation() {
        assertThatThrownBy(() ->
                command(AccountPolicy.selfServe()).register("gerald.durrell@oakvista.example", "hash"))
                .isInstanceOf(DuplicateEmailException.class);

        assertThat(violations("register.emailUnregistered")).isEqualTo(1);
        assertThat(baseline("register.emailUnregistered")).isZero();
    }

    @Test
    void register_newEmail_isObservedAsBaseline() {
        command(AccountPolicy.selfServe()).register("newbie@example.org", "hash");

        assertThat(baseline("register.emailUnregistered")).isEqualTo(1);
        assertThat(violations("register.emailUnregistered")).isZero();
        // The written account is observed on the success path (ADR-017 producer rule).
        assertThat(baseline("register.created.email")).isEqualTo(1);
    }

    @Test
    void verifyEmail_eachInvalidTokenCause_isObservedUnderItsOwnName() {
        AccountCommand cmd = command(AccountPolicy.selfServe());

        // unknown: never minted
        assertThatThrownBy(() -> cmd.verifyEmail("never-minted")).isInstanceOf(InvalidTokenException.class);
        assertThat(violations("verifyEmail.tokenKnown")).isEqualTo(1);

        // consumed: redeemed twice
        AccountCommand.Registration once = cmd.register("once@example.org", "hash");
        cmd.verifyEmail(once.rawToken());
        assertThatThrownBy(() -> cmd.verifyEmail(once.rawToken())).isInstanceOf(InvalidTokenException.class);
        assertThat(violations("verifyEmail.isConsumed")).isEqualTo(1);

        // expired: 25h after a 24h TTL
        AccountCommand.Registration late = cmd.register("late@example.org", "hash");
        AccountCommand later = commandAt(AccountPolicy.selfServe(), MINT_TIME.plusSeconds(25 * 3600));
        assertThatThrownBy(() -> later.verifyEmail(late.rawToken())).isInstanceOf(InvalidTokenException.class);
        assertThat(violations("verifyEmail.isExpired")).isEqualTo(1);

        // wrong purpose: a verification token offered as a reset
        AccountCommand.Registration mixed = cmd.register("mixed@example.org", "hash");
        assertThatThrownBy(() -> cmd.resetPassword(mixed.rawToken(), "new-hash")).isInstanceOf(InvalidTokenException.class);
        assertThat(violations("resetPassword.hasPurpose")).isEqualTo(1);

        // the message never distinguishes them; only the metric names do
        assertThat(violations("verifyEmail.hasPurpose")).isZero();
        assertThat(violations("resetPassword.tokenKnown")).isZero();
    }

    @Test
    void beginPasswordReset_unknownEmail_isObservedThoughTheResponseIsIdentical() {
        assertThat(command(AccountPolicy.selfServe()).beginPasswordReset("ghost@example.org")).isEmpty();

        assertThat(violations("beginPasswordReset.emailRegistered")).isEqualTo(1);
    }

    @Test
    void verifyEmail_suspendedAccount_isRejectedAndObserved() {
        AccountCommand cmd = command(AccountPolicy.selfServe());
        AccountCommand.Registration reg = cmd.register("banned@example.org", "hash");
        Account account = accounts.getByEmail("banned@example.org").orElseThrow();
        accounts.update(account.withStatus(AccountStatus.SUSPENDED));

        assertThatThrownBy(() -> cmd.verifyEmail(reg.rawToken())).isInstanceOf(InvalidTokenException.class);

        assertThat(violations("verifyEmail.accountActive")).isEqualTo(1);
        assertThat(accounts.getByEmail("banned@example.org").orElseThrow().access()).isEqualTo(AccessLevel.BROWSE_ONLY);
    }

    @Test
    void suspend_andReinstate_flipStatusAndAreObserved() {
        AccountCommand cmd = command(AccountPolicy.selfServe());
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
        assertThatThrownBy(() -> command(AccountPolicy.selfServe()).suspend(AccountName.create()))
                .isInstanceOf(AccountNotFoundException.class);

        assertThat(violations("suspend.accountKnown")).isEqualTo(1);
    }
}
