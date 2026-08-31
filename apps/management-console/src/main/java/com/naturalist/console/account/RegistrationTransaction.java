package com.naturalist.console.account;

import com.naturalist.account.AccountCommand;
import com.naturalist.account.AccountName;
import com.naturalist.naturalist.EcologicalStage;
import com.naturalist.naturalist.NaturalistCommand;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.naturalist.NaturalistRole;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Composes public self-registration across two domains at the app root — the one place
 * allowed to know both (a domain core never depends on another domain's core). One
 * {@link Transactional} boundary spans both writes: mint the auth {@code Account} (which
 * also mints its email-verification token) and create the linked ecological
 * {@code Naturalist}; either failing rolls back both, so registration can never leave a
 * half-built identity. {@code AccountCommand}'s own methods are independently transactional
 * and join this one by {@code REQUIRED} propagation.
 *
 * <p>Named a {@code Transaction}, not a {@code *Service} (CQS/transactional naming). It does
 * not send the verification email: that is a post-commit side effect the controller performs
 * on the returned {@link Result}, so a mail hiccup never rolls back a good registration.
 */
@Component
class RegistrationTransaction {

    /** A brand-new self-registrant is an ecological visitor, newly curious — not a child
     *  ({@code WONDER}) nor an established practitioner. */
    private static final NaturalistRole SELF_REGISTERED_ROLE = NaturalistRole.VISITOR;
    private static final EcologicalStage SELF_REGISTERED_STAGE = EcologicalStage.CURIOUS;

    private final AccountCommand accountCommand;
    private final NaturalistCommand naturalistCommand;
    private final PasswordEncoder passwordEncoder;

    RegistrationTransaction(AccountCommand accountCommand,
                            NaturalistCommand naturalistCommand,
                            PasswordEncoder passwordEncoder) {
        this.accountCommand = accountCommand;
        this.naturalistCommand = naturalistCommand;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new account + naturalist. The password is encoded here — the account port
     * never sees plaintext. Propagates {@code DuplicateEmailException} (email taken),
     * {@code InvariantViolationException} (malformed email, blank/short password, taken or
     * invalid handle) for the controller to turn into a form error.
     *
     * @return the minted handles, the login email, and the raw verification token to email.
     */
    @Transactional
    Result register(String email, String rawPassword, String publicHandle,
                    String givenName, @Nullable String familyName) {
        AccountCommand.Registration registration =
                accountCommand.register(email, passwordEncoder.encode(rawPassword));
        NaturalistName naturalist = naturalistCommand.create(
                registration.account(), publicHandle, givenName, familyName,
                SELF_REGISTERED_ROLE, SELF_REGISTERED_STAGE, null);
        return new Result(registration.account(), naturalist, email, registration.rawToken());
    }

    /** The outcome of {@link #register}: the minted handles, the login email, and the raw
     *  verification token to email (returned once, never stored in the clear). */
    record Result(AccountName account, NaturalistName naturalist, String email, String rawToken) {}
}
