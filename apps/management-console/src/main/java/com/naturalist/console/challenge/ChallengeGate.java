package com.naturalist.console.challenge;

import com.naturalist.challenge.ChallengeSubmission;
import com.naturalist.challenge.ChallengeVerdict;
import com.naturalist.challenge.ChallengeVerifier;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * What a form endpoint needs from the bot challenge: read the widget's token off the request and
 * get a verdict, and the site key to render the widget with. The web edge of the
 * {@link ChallengeVerifier} port (ADR-027 D6).
 *
 * <p>The token field is attacker-controlled and the port refuses an overlong one by throwing, so an
 * overlong field is a {@code FAILED} verdict here, before the port, rather than a 500. The address
 * passed on is the raw remote address, because the vendor checks it against the one that solved the
 * widget; {@code ClientKey} is a rate-limit key, which is a different thing.
 */
@Component
public class ChallengeGate {

    /** The form field the widget writes its token into. */
    static final String TOKEN_FIELD = "cf-turnstile-response";

    private final Observer observer = Observer.forClass(ChallengeGate.class, Observer.MonitoringMode.ALWAYS);
    private final ChallengeVerifier verifier;
    private final String siteKey;

    ChallengeGate(ChallengeVerifier verifier, ChallengeProperties properties) {
        this.verifier = verifier;
        this.siteKey = properties.siteKey() == null ? "" : properties.siteKey().strip();
    }

    /** The verdict for this request's token under {@code action}, which the widget also declares. */
    public ChallengeVerdict verify(HttpServletRequest request, String action) {
        String token = request.getParameter(TOKEN_FIELD);
        boolean withinLength = token == null || token.length() <= ChallengeSubmission.MAX_TOKEN_LENGTH;
        observer.forMethod("verify").state((Constraints c) -> c.isTrue(withinLength, "tokenWithinLength"))
                .observe(Level.WARN);
        if (!withinLength) {
            return ChallengeVerdict.failed("token-too-long");
        }
        return verifier.verify(ChallengeSubmission.of(token, action, request.getRemoteAddr()));
    }

    /** The site key for the widget; empty when none is configured, and then no widget renders. */
    public String siteKey() {
        return siteKey;
    }
}
