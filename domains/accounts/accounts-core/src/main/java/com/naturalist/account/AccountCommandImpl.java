package com.naturalist.account;

import com.naturalist.infrastructure.DomainService;
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
 */
@DomainService
class AccountCommandImpl implements AccountCommand {

    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofHours(1);

    private final Observer observer = Observer.forClass(getClass());

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

        if (accounts.getByEmail(email).isPresent()) {
            throw new DuplicateEmailException("email already registered");
        }

        AccountName name = AccountName.create();
        accounts.insert(new Account(
                name, email, passwordHash, false, AccessLevel.BROWSE_ONLY, AccountStatus.ACTIVE));

        SecureTokens.MintedToken minted = secureTokens.mint();
        tokens.insert(new EmailVerificationToken(
                EmailVerificationTokenId.create(),
                name,
                minted.hash(),
                TokenPurpose.VERIFY_EMAIL,
                clock.instant().plus(VERIFICATION_TTL),
                null));

        return new Registration(name, minted.rawValue());
    }

    @Override
    @Transactional
    public void verifyEmail(String rawToken) {
        observer.arguments("verifyEmail", i -> i.notBlank(rawToken, "rawToken"))
                .throwWhenInvalid();

        EmailVerificationToken token = tokens.getByTokenHash(secureTokens.hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("invalid token"));

        Instant now = clock.instant();
        if (token.purpose() != TokenPurpose.VERIFY_EMAIL
                || token.consumedAt() != null
                || now.isAfter(token.expiresAt())) {
            throw new InvalidTokenException("invalid token");
        }
        tokens.update(token.withConsumedAt(now));

        Account account = accounts.getByName(token.account())
                .orElseThrow(() -> new InvalidTokenException("invalid token"));
        Account verified = account.withEmailVerified(true);
        if (policy.grantsVisionOnVerification()) {
            verified = verified.withAccess(AccessLevel.VISION);
        }
        accounts.update(verified);
    }

    @Override
    public void grantVision(AccountName account) {
        observer.arguments("grantVision", i -> i.entityName(account, "account")).throwWhenInvalid();
        Account existing = accounts.getByName(account)
                .orElseThrow(() -> new AccountNotFoundException(account));
        accounts.update(existing.withAccess(AccessLevel.VISION));
    }

    @Override
    public void revokeVision(AccountName account) {
        observer.arguments("revokeVision", i -> i.entityName(account, "account")).throwWhenInvalid();
        Account existing = accounts.getByName(account)
                .orElseThrow(() -> new AccountNotFoundException(account));
        accounts.update(existing.withAccess(AccessLevel.BROWSE_ONLY));
    }

    @Override
    public Optional<String> beginPasswordReset(String email) {
        observer.arguments("beginPasswordReset", i -> i.email(email, "email")).throwWhenInvalid();
        Optional<Account> account = accounts.getByEmail(email);
        if (account.isEmpty()) {
            return Optional.empty();
        }
        SecureTokens.MintedToken minted = secureTokens.mint();
        tokens.insert(new EmailVerificationToken(
                EmailVerificationTokenId.create(),
                account.get().name(),
                minted.hash(),
                TokenPurpose.RESET_PASSWORD,
                clock.instant().plus(RESET_TTL),
                null));
        return Optional.of(minted.rawValue());
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPasswordHash) {
        observer.arguments("resetPassword", i -> i
                        .notBlank(rawToken, "rawToken")
                        .notBlank(newPasswordHash, "newPasswordHash"))
                .throwWhenInvalid();

        EmailVerificationToken token = tokens.getByTokenHash(secureTokens.hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("invalid token"));

        Instant now = clock.instant();
        if (token.purpose() != TokenPurpose.RESET_PASSWORD
                || token.consumedAt() != null
                || now.isAfter(token.expiresAt())) {
            throw new InvalidTokenException("invalid token");
        }
        tokens.update(token.withConsumedAt(now));

        Account account = accounts.getByName(token.account())
                .orElseThrow(() -> new InvalidTokenException("invalid token"));
        accounts.update(account.withPasswordHash(newPasswordHash));
    }
}
