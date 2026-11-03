package com.naturalist.challenge;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.function.Consumer;

/**
 * What a {@link ChallengeVerifier} found. {@code PASSED} and {@code FAILED} are definitive
 * answers about the submission; {@code UNAVAILABLE} means no answer could be had. Whether an
 * unavailable service lets the request through is the caller's decision, never the port's
 * (ADR-027 D6 fails closed). {@code reasons} are the vendor's error codes or the verifier's
 * own, for observation, and are never shown to the user.
 */
public record ChallengeVerdict(Outcome outcome, List<String> reasons) implements ValueObject {
    public enum Outcome {
        PASSED,
        FAILED,
        UNAVAILABLE
    }

    public static ChallengeVerdict passed() {
        return new ChallengeVerdict(Outcome.PASSED, List.of());
    }

    public static ChallengeVerdict failed(String... reasons) {
        return new ChallengeVerdict(Outcome.FAILED, List.of(reasons));
    }

    public static ChallengeVerdict unavailable(String... reasons) {
        return new ChallengeVerdict(Outcome.UNAVAILABLE, List.of(reasons));
    }

    public boolean isPassed() {
        return outcome == Outcome.PASSED;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return (Constraints c) -> c
                .notNull(outcome, "outcome")
                .notNull(reasons, "reasons");
    }
}
