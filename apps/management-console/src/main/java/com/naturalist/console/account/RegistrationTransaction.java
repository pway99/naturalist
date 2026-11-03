package com.naturalist.console.account;

import com.naturalist.account.AccountCommand;
import com.naturalist.account.AccountName;
import com.naturalist.ddd.EmailAddress;
import com.naturalist.naturalist.EcologicalStage;
import com.naturalist.naturalist.NaturalistCommand;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.naturalist.NaturalistRole;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.RateLimitExceededException;
import com.naturalist.resilience.RateLimiter;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Resilient;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Composes public self-registration across two domains at the app root — the one place
 * allowed to know both (a domain core never depends on another domain's core). One
 * {@link Transactional} boundary spans both writes: mint the auth {@code Account} (which
 * also mints its email-verification token) and create the linked ecological
 * {@code Naturalist}; either failing rolls back both, so registration can never leave a
 * half-built identity. {@code AccountCommand}'s own methods are independently transactional
 * and join this one by {@code REQUIRED} propagation.
 *
 * <p>Like every command, query and {@code Transaction} in the system it observes its
 * arguments first ({@code observer.arguments("register", ...)}) and throws
 * {@code InvariantViolationException} before any work: before the password is encoded,
 * before either domain is written. That is what makes a bad form submission attributable
 * to registration in the metrics (class {@code RegistrationTransaction}, method
 * {@code register}) rather than to whichever downstream command happened to notice. It is
 * also the only place the minimum password length can be enforced — the account port only
 * ever sees the encoded hash — so that rule lives here as an observed constraint, not as a
 * controller {@code if}.
 *
 * <p>Named a {@code Transaction}, not a {@code *Service} (CQS/transactional naming). It does
 * not extend the kernel {@code data.Transaction}: that template persists one
 * {@code Aggregate} and returns nothing, whereas this composes two domains' ports and must
 * hand back the raw verification token. It does not send the verification email: that is a
 * post-commit side effect the controller performs on the returned {@link Result}, so a mail
 * hiccup never rolls back a good registration.
 */
@Component
@Resilient(name = RegistrationTransaction.RATE_STRATEGY)
class RegistrationTransaction {

    /** Global sign-up ceiling (see {@code ResilienceConfiguration}): registration sends email to
     *  an unproven address, so an unbounded endpoint is a mail-bombing primitive. */
    static final String RATE_STRATEGY = "auth.register";

    /** Per-client sign-up ceiling, keyed by the {@code clientKey} the controller derives from the
     *  request ({@code ClientKey}); taken before the global one so a single script never spends
     *  the system-wide budget. */
    static final String CLIENT_RATE_STRATEGY = "auth.register.client";

    /** Server-side floor for a self-chosen password; the form's {@code minlength} mirrors it. */
    static final int MIN_PASSWORD_LENGTH = 8;

    /**
     * Server-side ceiling, in UTF-8 bytes: BCrypt rejects anything longer with an
     * {@code IllegalArgumentException}, which uncaught is a 500 on the registration form.
     * Observed as a constraint so it is a 4xx with a metric instead.
     */
    static final int MAX_PASSWORD_BYTES = 72;

    /** A brand-new self-registrant is an ecological visitor, newly curious — not a child
     *  ({@code WONDER}) nor an established practitioner. */
    private static final NaturalistRole SELF_REGISTERED_ROLE = NaturalistRole.VISITOR;
    private static final EcologicalStage SELF_REGISTERED_STAGE = EcologicalStage.CURIOUS;

    /** ALWAYS: registration is an attack surface — the valid rows are the baseline the violations stand out against. */
    private final Observer observer = Observer.forClass(getClass(), Observer.MonitoringMode.ALWAYS);
    private final AccountCommand accountCommand;
    private final NaturalistCommand naturalistCommand;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter registrations;
    private final RateLimiter registrationsPerClient;

    RegistrationTransaction(AccountCommand accountCommand,
                            NaturalistCommand naturalistCommand,
                            PasswordEncoder passwordEncoder,
                            Resilience resilience) {
        this.accountCommand = accountCommand;
        this.naturalistCommand = naturalistCommand;
        this.passwordEncoder = passwordEncoder;
        this.registrations = resilience.rateLimiter(RATE_STRATEGY);
        this.registrationsPerClient = resilience.rateLimiter(CLIENT_RATE_STRATEGY);
    }

    /**
     * Registers a new account + naturalist. The password is encoded here — the account port
     * never sees plaintext. Throws {@code InvariantViolationException} first for any invalid
     * input (malformed email, blank or too-short password, blank handle or given name) — its
     * {@code violations()} name the failing fields ({@code email}, {@code password},
     * {@code password.length}, {@code publicHandle}, {@code givenName}) for the controller to
     * render per field — and propagates {@code DuplicateEmailException} (email taken) and the
     * naturalists domain's own {@code InvariantViolationException} (taken handle).
     *
     * <p>Rate limits come first, before any validation or BCrypt work — an attacker's attempts,
     * valid or not, are what the ceilings bound. The client's own budget
     * ({@code auth.register.client}, keyed by {@code clientKey}) is spent before the system-wide
     * one ({@code auth.register}), so one script is refused alone and never drains the budget
     * honest sign-ups share. Either refusal is a {@code RateLimitExceededException} naming its
     * strategy; the client key is logged by the caller, never tagged.
     *
     * @param clientKey who is asking, as {@code ClientKey.of(request)} spells it — opaque here.
     * @return the minted handles, the login email, and the raw verification token to email.
     */
    @Transactional
    Result register(String clientKey, EmailAddress email, String rawPassword, String publicHandle,
                    String givenName, @Nullable String familyName) {
        MethodObserver register = observer.forMethod("register");

        register.arguments(i -> i.notBlank(clientKey, "clientKey")).throwWhenInvalid();
        boolean clientPermitted = acquirePermit(registrationsPerClient.forKey(clientKey));
        boolean permitted = !clientPermitted || acquirePermit(registrations);
        register.arguments(i -> i
                        .isTrue(clientPermitted, "withinClientRate")
                        .isTrue(permitted, "withinRate"))
                .observe(Level.WARN);
        if (!clientPermitted) {
            throw new RateLimitExceededException("registration rate limit exceeded for this client", CLIENT_RATE_STRATEGY);
        }
        if (!permitted) {
            throw new RateLimitExceededException("registration rate limit exceeded", RATE_STRATEGY);
        }

        register.arguments(i -> i
                        .emailAddress(email, "email")
                        .notBlank(rawPassword, "password")
                        .whenNotNull(rawPassword, c -> c
                                .atLeast(rawPassword.length(), MIN_PASSWORD_LENGTH, "password.length")
                                .atMost(rawPassword.getBytes(StandardCharsets.UTF_8).length,
                                        MAX_PASSWORD_BYTES, "password.bytes"))
                        .notBlank(publicHandle, "publicHandle")
                        .notBlank(givenName, "givenName"))
                .throwWhenInvalid();

        AccountCommand.Registration registration =
                accountCommand.register(email.value(), passwordEncoder.encode(rawPassword));
        NaturalistName naturalist = naturalistCommand.create(
                registration.account(), publicHandle, givenName, familyName,
                SELF_REGISTERED_ROLE, SELF_REGISTERED_STAGE, null);
        return new Result(registration.account(), naturalist, email, registration.rawToken());
    }

    private static boolean acquirePermit(RateLimiter limiter) {
        try {
            limiter.execute(() -> { });
            return true;
        } catch (RateLimitExceededException rejected) {
            return false;
        }
    }

    /** The outcome of {@link #register}: the minted handles, the login email, and the raw
     *  verification token to email (returned once, never stored in the clear). */
    record Result(AccountName account, NaturalistName naturalist, EmailAddress email, String rawToken) {}
}
