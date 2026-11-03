package com.naturalist.challenge;

import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Direct kernel test under the "no immediate consumer" rule: the port lands ahead of the
 * registration and login wiring that will exercise it (ADR-027 PRs 5 and 6).
 */
class LogOnlyChallengeVerifierTest {

    private final ChallengeVerifier verifier = new LogOnlyChallengeVerifier();

    @Test
    void passes_aSubmissionCarryingAToken() {
        ChallengeVerdict verdict = verifier.verify(ChallengeSubmission.of("a-token", "register", "203.0.113.7"));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.PASSED);
        assertThat(verdict.reasons()).isEmpty();
    }

    @Test
    void passes_aSubmissionWithNoToken_becauseNoWidgetRendersInThisMode() {
        ChallengeVerdict verdict = verifier.verify(ChallengeSubmission.of(null, "login", null));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.PASSED);
    }

    @Test
    void refuses_aMissingSubmission() {
        assertThatThrownBy(() -> verifier.verify(null)).isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void refuses_aSubmissionWithABlankAction() {
        ChallengeSubmission noAction = ChallengeSubmission.of("a-token", " ", null);

        assertThatThrownBy(() -> verifier.verify(noAction)).isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void refuses_aTokenLongerThanTheVendorMaximum() {
        ChallengeSubmission overlong = ChallengeSubmission.of("x".repeat(ChallengeSubmission.MAX_TOKEN_LENGTH + 1), "login", null);

        assertThatThrownBy(() -> verifier.verify(overlong)).isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void accepts_aTokenAtTheVendorMaximum() {
        ChallengeSubmission longest = ChallengeSubmission.of("x".repeat(ChallengeSubmission.MAX_TOKEN_LENGTH), "login", null);

        assertThat(verifier.verify(longest).outcome()).isEqualTo(ChallengeVerdict.Outcome.PASSED);
    }

    @Test
    void verdict_factoriesCarryTheirReasons() {
        assertThat(ChallengeVerdict.passed().isPassed()).isTrue();
        assertThat(ChallengeVerdict.failed("timeout-or-duplicate").outcome()).isEqualTo(ChallengeVerdict.Outcome.FAILED);
        assertThat(ChallengeVerdict.failed("timeout-or-duplicate").reasons()).containsExactly("timeout-or-duplicate");
        assertThat(ChallengeVerdict.unavailable("internal-error").outcome()).isEqualTo(ChallengeVerdict.Outcome.UNAVAILABLE);
        assertThat(ChallengeVerdict.unavailable("internal-error").isPassed()).isFalse();
    }

    @Test
    void submission_toStringDoesNotExposeTheToken() {
        assertThat(ChallengeSubmission.of("a-token", "login", null).toString()).doesNotContain("a-token");
    }
}
