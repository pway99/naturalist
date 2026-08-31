package com.naturalist.account;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Plain {@code @DomainService} write command (ADR-010 style; mirrors {@code UsageCommandImpl}).
 * Time comes from an injected {@link Clock} and secrets from an injected {@link SecureTokens}, so
 * expiry and token behaviour are deterministically testable. Register writes an account then a
 * token; atomicity across the two is the RDBMS adapter's transaction boundary (the in-memory mock
 * needs none).
 */
@DomainService
class AccountCommandImpl implements AccountCommand {

    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);

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
}
