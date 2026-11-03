package com.naturalist.challenge;

import com.naturalist.observability.Observer;

/**
 * Dev and test {@link ChallengeVerifier}: passes everything, including a submission with no
 * token, because the templates render no widget in this mode. Selected only by the explicit
 * {@code naturalist.challenge.log-only=true} opt-in, never as a fallback (ADR-027 D6). Logs
 * the action alone: the token is a bearer value and the client address is not logged at all.
 *
 * <p>Uses the JDK {@link System.Logger} so the kernel keeps its dependency budget, like
 * {@code LoggingEmailSender}.
 */
public final class LogOnlyChallengeVerifier implements ChallengeVerifier {
    private static final System.Logger LOG = System.getLogger(LogOnlyChallengeVerifier.class.getName());

    private final Observer observer = Observer.forClass(getClass());

    @Override
    public ChallengeVerdict verify(ChallengeSubmission submission) {
        observer.arguments("verify", i -> i.valueObject(submission, "submission"))
                .throwWhenInvalid();
        LOG.log(System.Logger.Level.INFO, "[challenge:log-only] no verifier configured; passing action {0}.", submission.action());
        return ChallengeVerdict.passed();
    }
}
