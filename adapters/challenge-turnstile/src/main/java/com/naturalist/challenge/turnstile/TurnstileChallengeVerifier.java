package com.naturalist.challenge.turnstile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.challenge.ChallengeSubmission;
import com.naturalist.challenge.ChallengeVerdict;
import com.naturalist.challenge.ChallengeVerifier;
import com.naturalist.ddd.EntityId;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Resilient;
import com.naturalist.secrets.Secret;
import com.naturalist.secrets.SecretName;
import com.naturalist.secrets.SecretProvider;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * {@link ChallengeVerifier} over the Turnstile siteverify endpoint, on the JDK HTTP client
 * (ADR-027 D6). Only reports: a definitive vendor answer is {@code PASSED} or {@code FAILED},
 * and a timeout, an open breaker, a server error or an unreadable body is {@code UNAVAILABLE}.
 * The caller decides what an unavailable service means.
 *
 * <p>The nesting is deliberate. The timeout sits outside the retry so {@code challenge.verify}'s
 * ceiling bounds the whole verification, and the breaker sits outside both so one verification
 * counts once. {@code exchange} returns for a definitive answer and throws only for what is
 * worth retrying, so a rejected token never reaches the breaker. One idempotency key spans the
 * attempts, so a retried request cannot burn the single-use token.
 *
 * <p>The credential is the {@link #CREDENTIAL} secret, resolved once at construction (ADR-028).
 * The token is a bearer value and neither it nor the credential is ever logged.
 */
@Resilient(name = TurnstileChallengeVerifier.STRATEGY)
public final class TurnstileChallengeVerifier implements ChallengeVerifier {
    /** The one secret this adapter resolves, at construction (ADR-028). */
    public static final SecretName CREDENTIAL = SecretName.of("challenge-credential");

    /** The resilience strategy the composition root must register. */
    static final String STRATEGY = "challenge.verify";

    private static final Duration ATTEMPT_TIMEOUT = Duration.ofMillis(900);

    private static final String INTERNAL_ERROR = "internal-error";

    private static final Set<String> REJECTED_CREDENTIAL_CODES = Set.of("missing-input-secret", "invalid-input-secret");

    private static final String ACTION_MISMATCH = "action-mismatch";

    private static final String HOSTNAME_MISMATCH = "hostname-mismatch";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final System.Logger LOG = System.getLogger(TurnstileChallengeVerifier.class.getName());

    private final Observer observer = Observer.forClass(getClass(), Observer.MonitoringMode.ALWAYS);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(ATTEMPT_TIMEOUT).build();

    private final TurnstileConfig config;

    private final Secret credential;

    private final Resilience resilience;

    public TurnstileChallengeVerifier(TurnstileConfig config, SecretProvider secrets, Resilience resilience) {
        observer.arguments("constructor", i -> i
                .valueObject(config, "config")
                .notNull(secrets, "secrets")
                .notNull(resilience, "resilience")
        ).throwWhenInvalid();
        this.config = config;
        this.credential = secrets.require(CREDENTIAL);
        this.resilience = resilience;
    }

    @Override
    public ChallengeVerdict verify(ChallengeSubmission submission) {
        MethodObserver m = observer.forMethod("verify");
        m.arguments(i -> i.valueObject(submission, "submission")).throwWhenInvalid();
        ChallengeVerdict verdict = StringUtils.isBlank(submission.token())
                ? ChallengeVerdict.failed("missing-token")
                : verifyWithVendor(submission);
        observeDecision(m, verdict);
        return verdict;
    }

    private ChallengeVerdict verifyWithVendor(ChallengeSubmission submission) {
        String idempotencyKey = EntityId.newUUID().toString();
        try {
            return resilience.circuitBreaker(STRATEGY).execute(() ->
                    resilience.timeout(STRATEGY).execute(() ->
                            resilience.retry(STRATEGY).execute(() -> exchange(submission, idempotencyKey))));
        } catch (RuntimeException e) {
            // Covers the facade's own timeout and open-breaker exceptions, whose types belong to the vendor adapter.
            LOG.log(System.Logger.Level.WARNING, "[challenge] verification unavailable: {0}", e.getClass().getSimpleName());
            return ChallengeVerdict.unavailable("service-unavailable");
        }
    }

    private ChallengeVerdict exchange(ChallengeSubmission submission, String idempotencyKey) {
        HttpResponse<String> response = send(submission, idempotencyKey);
        if (response.statusCode() >= 500) {
            throw new TransientFailure("status " + response.statusCode(), null);
        }
        JsonNode body = parse(response.body());
        if (!body.path("success").asBoolean()) {
            List<String> codes = errorCodes(body);
            if (codes.contains(INTERNAL_ERROR)) {
                throw new TransientFailure(INTERNAL_ERROR, null);
            }
            return ChallengeVerdict.failed(codes.toArray(String[]::new));
        }
        if (!submission.action().equals(body.path("action").asText())) {
            return ChallengeVerdict.failed(ACTION_MISMATCH);
        }
        if (hostnameExpected() && !config.expectedHostname().equalsIgnoreCase(body.path("hostname").asText())) {
            return ChallengeVerdict.failed(HOSTNAME_MISMATCH);
        }
        return ChallengeVerdict.passed();
    }

    private HttpResponse<String> send(ChallengeSubmission submission, String idempotencyKey) {
        String form = "secret=" + encode(credential.value())
                + "&response=" + encode(submission.token())
                + "&idempotency_key=" + encode(idempotencyKey)
                + (submission.clientAddress() == null ? "" : "&remoteip=" + encode(submission.clientAddress()));
        HttpRequest request = HttpRequest.newBuilder(config.endpoint())
                .timeout(ATTEMPT_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new TransientFailure("transport", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransientFailure("interrupted", e);
        }
    }

    private static JsonNode parse(String body) {
        try {
            JsonNode node = MAPPER.readTree(body);
            if (!node.path("success").isBoolean()) {
                throw new TransientFailure("no verdict in the response", null);
            }
            return node;
        } catch (JsonProcessingException e) {
            throw new TransientFailure("unreadable response", null);
        }
    }

    private static List<String> errorCodes(JsonNode body) {
        List<String> codes = new ArrayList<>();
        body.path("error-codes").forEach(code -> codes.add(code.asText()));
        return codes;
    }

    private void observeDecision(MethodObserver m, ChallengeVerdict verdict) {
        List<String> reasons = verdict.reasons();
        m.state((Constraints c) -> c
                .isTrue(verdict.outcome() != ChallengeVerdict.Outcome.FAILED, "challengePassed")
                .isTrue(verdict.outcome() != ChallengeVerdict.Outcome.UNAVAILABLE, "serviceAvailable")
                .isTrue(!reasons.contains(ACTION_MISMATCH), "actionMatches")
                .isTrue(!reasons.contains(HOSTNAME_MISMATCH), "hostnameMatches")
                .isTrue(hostnameExpected(), "hostnameChecked")
                .isTrue(Collections.disjoint(reasons, REJECTED_CREDENTIAL_CODES), "credentialAccepted"))
                .observe(Level.WARN);
    }

    private boolean hostnameExpected() {
        return StringUtils.isNotBlank(config.expectedHostname());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Worth retrying, and counted by the breaker. The message never carries the request. */
    private static final class TransientFailure extends RuntimeException {
        TransientFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
