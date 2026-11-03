package com.naturalist.challenge;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * What a browser submitted for a challenge: the widget's {@code token}, or null when the
 * form carried none, the {@code action} the page was rendered for, and the client address
 * the app resolved (ADR-027 D3), when it has one. Refused at the port if the action is blank
 * or the token exceeds the vendor maximum, so a hostile form cannot inflate an outbound call.
 *
 * <p>{@link #toString()} leaves the token out: it is a bearer value until redeemed.
 */
public record ChallengeSubmission(@Nullable String token, String action, @Nullable String clientAddress)
        implements ValueObject {

    /** The longest token a widget issues. */
    public static final int MAX_TOKEN_LENGTH = 2048;

    public static ChallengeSubmission of(@Nullable String token, String action, @Nullable String clientAddress) {
        return new ChallengeSubmission(token, action, clientAddress);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return (Constraints c) -> c
                .notBlank(action, "action")
                .whenNotNull(token, t -> t.maxLength(token, MAX_TOKEN_LENGTH, "token"));
    }

    @Override
    public String toString() {
        return "ChallengeSubmission[action=" + action + ", tokenPresent=" + (token != null) + "]";
    }
}
