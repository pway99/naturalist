package com.naturalist.account;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Plain {@code @DomainService} write command (ADR-010 style; mirrors {@code UsageCommandImpl}).
 * Time comes from an injected {@link Clock} and secrets from an injected {@link SecureTokens}, so
 * expiry and token behaviour are deterministically testable.
 *
 * <p>{@code register} and {@code verifyEmail} each write more than one entity, so both carry
 * {@link Transactional @jakarta.transaction.Transactional} — the same annotation the framework's
 * {@code Transaction} base uses. Under {@code @DomainService} the runtime adapter applies the
 * transaction manager, so the coordinated writes commit or roll back as a unit; the in-memory mock
 * is non-transactional, which is correct for unit tests. The app's {@code RegistrationTransaction}
 * remains the outer boundary that also spans the cross-domain {@code Naturalist} write.
 *
 * <h2>Observability — every decision, not just every argument</h2>
 * Accounts are the attack surface, so this command runs its {@link Observer} in
 * {@link Observer.MonitoringMode#ALWAYS}: every constraint inspected emits, valid or not. Each
 * business decision is expressed as a predicate-named constraint and observed at
 * {@link Level#WARN} <em>before</em> the exception the port already throws — the thrown message
 * never changes, so nothing new leaks to a caller, while the metric tags say exactly which rule
 * failed: {@code AccountCommandImpl.register.emailUnregistered} is an enumeration probe,
 * {@code …verifyEmail.tokenKnown} is token guessing, {@code …isConsumed} is a replay,
 * {@code …isExpired} is a slow user, {@code …accountKnown} is a data-integrity hole. The same
 * names with {@code valid=true} are the baseline rates those anomalies stand out against. The
 * written entity is observed on every success path (ADR-017's producer rule).
 */
@DomainService
class AccountCommandImpl implements AccountCommand {

    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofHours(1);

    private final Observer observer = Observer.forClass(getClass(), Observer.MonitoringMode.ALWAYS);

    private final AccountRepository.AccountEntityRepository accounts;
    private final AccountRepository.VerificationTokenRepository tokens;
    private final SecureTokens secureTokens;
    private final Clock clock;
    private final AccountPolicy policy;

    AccountCommandImpl(AccountRepository.AccountEntityRepository accounts,
                       AccountRepository.VerificationTokenRepository tokens,
                       SecureTokens secureTokens,
                       Clock clock,
                       AccountPolicy policy) {
        observer.arguments("constructor", i -> i
                        .notNull(accounts, "accounts")
                        .notNull(tokens, "tokens")
                        .notNull(secureTokens, "secureTokens")
                        .notNull(clock, "clock")
                        .notNull(policy, "policy"))
                .throwWhenInvalid();
        this.accounts = accounts;
        this.tokens = tokens;
        this.secureTokens = secureTokens;
        this.clock = clock;
        this.policy = policy;
    }

    @Override
    @Transactional
    public Registration register(String email, String passwordHash) {
        observer.arguments("register", i -> i
                        .email(email, "email")
                        .notBlank(passwordHash, "passwordHash"))
                .throwWhenInvalid();

        Optional<Account> existing = accounts.getByEmail(email);
        InvariantObservation decision = observer.forMethod("register")
                .arguments(i -> i.isTrue(existing.isEmpty(), "emailUnregistered"))
                .observe(Level.WARN);
        if (!decision.violations().isEmpty()) {
            throw new DuplicateEmailException("email already registered");
        }

        AccountName name = AccountName.create();
        Account created = new Account(
                name, email, passwordHash, false, AccessLevel.BROWSE_ONLY, AccountStatus.ACTIVE);
        accounts.insert(created);

        SecureTokens.MintedToken minted = secureTokens.mint();
        EmailVerificationToken token = new EmailVerificationToken(
                EmailVerificationTokenId.create(),
                name,
                minted.hash(),
                TokenPurpose.VERIFY_EMAIL,
                clock.instant().plus(VERIFICATION_TTL),
                null);
        tokens.insert(token);

        observer.forMethod("register").namedEntity(created, "created").observe(Level.WARN);
        observer.forMethod("register").namedEntity(token, "verificationToken").observe(Level.WARN);
        return new Registration(name, minted.rawValue());
    }

    @Override
    @Transactional
    public void verifyEmail(String rawToken) {
        observer.arguments("verifyEmail", i -> i.notBlank(rawToken, "rawToken"))
                .throwWhenInvalid();

        Instant now = clock.instant();
        EmailVerificationToken token = redeemableToken("verifyEmail", rawToken, TokenPurpose.VERIFY_EMAIL, now);
        tokens.update(token.withConsumedAt(now));

        Account account = activeAccount("verifyEmail", token.account());
        Account verified = account.withEmailVerified(true);
        if (policy.grantsVisionOnVerification()) {
            verified = verified.withAccess(AccessLevel.VISION);
        }
        accounts.update(verified);
        observer.forMethod("verifyEmail").namedEntity(verified, "verified").observe(Level.WARN);
    }

    @Override
    public void grantVision(AccountName account) {
        observer.arguments("grantVision", i -> i.entityName(account, "account")).throwWhenInvalid();
        Account existing = knownAccount("grantVision", account)
                .orElseThrow(() -> new AccountNotFoundException(account));
        Account granted = existing.withAccess(AccessLevel.VISION);
        accounts.update(granted);
        observer.forMethod("grantVision").namedEntity(granted, "granted").observe(Level.WARN);
    }

    @Override
    public void revokeVision(AccountName account) {
        observer.arguments("revokeVision", i -> i.entityName(account, "account")).throwWhenInvalid();
        Account existing = knownAccount("revokeVision", account)
                .orElseThrow(() -> new AccountNotFoundException(account));
        Account revoked = existing.withAccess(AccessLevel.BROWSE_ONLY);
        accounts.update(revoked);
        observer.forMethod("revokeVision").namedEntity(revoked, "revoked").observe(Level.WARN);
    }

    @Override
    public Optional<String> beginPasswordReset(String email) {
        observer.arguments("beginPasswordReset", i -> i.email(email, "email")).throwWhenInvalid();
        Optional<Account> account = accounts.getByEmail(email);
        // The caller answers identically either way (no enumeration through the response);
        // the metric is the only place an unknown email is distinguishable — which is the point.
        InvariantObservation decision = observer.forMethod("beginPasswordReset")
                .arguments(i -> i.isTrue(account.isPresent(), "emailRegistered"))
                .observe(Level.WARN);
        if (!decision.violations().isEmpty()) {
            return Optional.empty();
        }
        SecureTokens.MintedToken minted = secureTokens.mint();
        EmailVerificationToken token = new EmailVerificationToken(
                EmailVerificationTokenId.create(),
                account.get().name(),
                minted.hash(),
                TokenPurpose.RESET_PASSWORD,
                clock.instant().plus(RESET_TTL),
                null);
        tokens.insert(token);
        observer.forMethod("beginPasswordReset").namedEntity(token, "resetToken").observe(Level.WARN);
        return Optional.of(minted.rawValue());
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPasswordHash) {
        observer.arguments("resetPassword", i -> i
                        .notBlank(rawToken, "rawToken")
                        .notBlank(newPasswordHash, "newPasswordHash"))
                .throwWhenInvalid();

        Instant now = clock.instant();
        EmailVerificationToken token = redeemableToken("resetPassword", rawToken, TokenPurpose.RESET_PASSWORD, now);
        tokens.update(token.withConsumedAt(now));

        Account account = activeAccount("resetPassword", token.account());
        Account reset = account.withPasswordHash(newPasswordHash);
        accounts.update(reset);
        observer.forMethod("resetPassword").namedEntity(reset, "passwordReset").observe(Level.WARN);
    }

    /**
     * Resolves a raw token to one that may be redeemed for {@code purpose} now, observing the
     * four ways it can fail under four names — {@code tokenKnown}, {@code hasPurpose},
     * {@code isConsumed}, {@code isExpired} — while throwing the one unchanged
     * {@link InvalidTokenException} message, so the caller learns nothing the metrics do.
     */
    private EmailVerificationToken redeemableToken(String method, String rawToken,
                                                   TokenPurpose purpose, Instant now) {
        Optional<EmailVerificationToken> found = tokens.getByTokenHash(secureTokens.hash(rawToken));
        EmailVerificationToken token = found.orElse(null);
        InvariantObservation decision = observer.forMethod(method)
                .arguments(i -> i
                        .isTrue(found.isPresent(), "tokenKnown")
                        .whenNotNull(token, c -> c
                                .isTrue(token.hasPurpose(purpose), "hasPurpose")
                                .isFalse(token.isConsumed(), "isConsumed")
                                .isFalse(token.isExpired(now), "isExpired")))
                .observe(Level.WARN);
        if (!decision.violations().isEmpty()) {
            throw new InvalidTokenException("invalid token");
        }
        return token;
    }

    /**
     * The token's owner, which must exist and be {@code ACTIVE} to redeem it — a suspended
     * account may not verify its way into VISION or reset its way back in. Observed as
     * {@code accountKnown} and {@code accountActive}; either failing is the same unchanged
     * {@link InvalidTokenException} to the caller.
     */
    private Account activeAccount(String method, AccountName name) {
        Account account = knownAccount(method, name)
                .orElseThrow(() -> new InvalidTokenException("invalid token"));
        InvariantObservation decision = observer.forMethod(method)
                .arguments(i -> i.isTrue(account.status() == AccountStatus.ACTIVE, "accountActive"))
                .observe(Level.WARN);
        if (!decision.violations().isEmpty()) {
            throw new InvalidTokenException("invalid token");
        }
        return account;
    }

    /** Looks an account up by name, observing a miss as {@code accountKnown}; the caller picks the exception. */
    private Optional<Account> knownAccount(String method, AccountName name) {
        Optional<Account> account = accounts.getByName(name);
        observer.forMethod(method)
                .arguments(i -> i.isTrue(account.isPresent(), "accountKnown"))
                .observe(Level.WARN);
        return account;
    }
}
