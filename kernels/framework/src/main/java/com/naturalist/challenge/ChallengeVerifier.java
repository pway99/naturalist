package com.naturalist.challenge;

/**
 * Port for proving a request came through a human-verification widget (ADR-027 D6). Vendor
 * implementations live under {@code adapters/}; {@link LogOnlyChallengeVerifier} is the
 * explicit dev and test opt-in. The port only reports: {@link ChallengeVerdict.Outcome#UNAVAILABLE}
 * is an answer, not an exception, and the caller decides what it means.
 */
public interface ChallengeVerifier {
    ChallengeVerdict verify(ChallengeSubmission submission);
}
