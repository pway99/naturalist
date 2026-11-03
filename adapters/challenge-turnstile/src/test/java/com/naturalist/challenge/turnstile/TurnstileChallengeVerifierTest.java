package com.naturalist.challenge.turnstile;

import com.naturalist.challenge.ChallengeSubmission;
import com.naturalist.challenge.ChallengeVerdict;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.resilience.Resilience;
import com.naturalist.secrets.EnvironmentSecretProvider;
import com.naturalist.secrets.SecretProvider;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real HTTP against a JDK stub server bound to an ephemeral port, so the form encoding, the
 * status handling and the per-attempt client timeout are all exercised. No account and no
 * network. The resilience behaviour runs on {@link TestResilience}, a stand-in: proving the
 * adapter against the vendor implementation is owed by PR 5 (see the ADR-027 plan), because
 * an adapter may not depend on another adapter.
 */
class TurnstileChallengeVerifierTest {

    private static final String HOSTNAME = "backyardnaturalist.app";
    private static final String PASS_BODY =
            "{\"success\":true,\"action\":\"register\",\"hostname\":\"" + HOSTNAME + "\"}";

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final SecretProvider secrets =
            new EnvironmentSecretProvider(Map.of("CHALLENGE_CREDENTIAL", "not-a-real-secret"));
    private final Deque<StubResponse> script = new ArrayDeque<>();
    private final List<Map<String, String>> requests = new CopyOnWriteArrayList<>();

    private HttpServer server;
    private URI endpoint;

    @BeforeEach
    void setUp() throws IOException {
        Metrics.addRegistry(meters);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/siteverify", exchange -> {
            requests.add(form(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            StubResponse response = script.size() > 1 ? script.poll() : script.peek();
            sleep(response.delay());
            byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(response.status(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        Metrics.removeRegistry(meters);
    }

    @Test
    void success_withMatchingActionAndHostname_passes() {
        respond(200, PASS_BODY);

        ChallengeVerdict verdict = verifier(Resilience.noOp()).verify(submission("a-token"));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.PASSED);
        assertThat(violations("challengePassed")).isZero();
    }

    @Test
    void request_carriesTheCredentialTokenAndAddressAsAForm() {
        respond(200, PASS_BODY);

        verifier(Resilience.noOp()).verify(ChallengeSubmission.of("a token&more", "register", "203.0.113.7"));

        assertThat(requests).singleElement().satisfies(form -> {
            assertThat(form).containsEntry("secret", "not-a-real-secret");
            assertThat(form).containsEntry("response", "a token&more");
            assertThat(form).containsEntry("remoteip", "203.0.113.7");
            assertThat(form).containsKey("idempotency_key");
        });
    }

    @Test
    void request_omitsTheAddressWhenThereIsNone() {
        respond(200, PASS_BODY);

        verifier(Resilience.noOp()).verify(ChallengeSubmission.of("a-token", "register", null));

        assertThat(requests.getFirst()).doesNotContainKey("remoteip");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-input-response", "timeout-or-duplicate", "missing-input-response", "bad-request"})
    void definitiveErrorCode_failsWithThatReason_withoutARetry(String code) {
        respond(200, "{\"success\":false,\"error-codes\":[\"" + code + "\"]}");

        ChallengeVerdict verdict = verifier(TestResilience.retrying(2)).verify(submission("a-token"));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.FAILED);
        assertThat(verdict.reasons()).containsExactly(code);
        assertThat(requests).hasSize(1);
        assertThat(violations("challengePassed")).isEqualTo(1);
        assertThat(violations("serviceAvailable")).isZero();
    }

    @Test
    void definitiveErrorCode_onA4xxStatus_isStillAnAnswer() {
        respond(400, "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}");

        ChallengeVerdict verdict = verifier(TestResilience.retrying(2)).verify(submission("a-token"));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.FAILED);
        assertThat(verdict.reasons()).containsExactly("invalid-input-response");
        assertThat(requests).hasSize(1);
    }

    @Test
    void rejectedCredential_failsAndIsObservedSeparately_soARotationMistakeDoesNotLookLikeBots() {
        respond(200, "{\"success\":false,\"error-codes\":[\"invalid-input-secret\"]}");

        ChallengeVerdict verdict = verifier(Resilience.noOp()).verify(submission("a-token"));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.FAILED);
        assertThat(violations("credentialAccepted")).isEqualTo(1);
    }

    @Test
    void actionMismatch_fails() {
        respond(200, "{\"success\":true,\"action\":\"login\",\"hostname\":\"" + HOSTNAME + "\"}");

        ChallengeVerdict verdict = verifier(Resilience.noOp()).verify(submission("a-token"));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.FAILED);
        assertThat(verdict.reasons()).containsExactly("action-mismatch");
        assertThat(violations("actionMatches")).isEqualTo(1);
    }

    @Test
    void hostnameMismatch_fails() {
        respond(200, "{\"success\":true,\"action\":\"register\",\"hostname\":\"elsewhere.example\"}");

        ChallengeVerdict verdict = verifier(Resilience.noOp()).verify(submission("a-token"));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.FAILED);
        assertThat(verdict.reasons()).containsExactly("hostname-mismatch");
        assertThat(violations("hostnameMatches")).isEqualTo(1);
    }

    @Test
    void noExpectedHostname_skipsTheCheck_andKeepsAStandingViolation() {
        respond(200, "{\"success\":true,\"action\":\"register\",\"hostname\":\"elsewhere.example\"}");

        ChallengeVerdict verdict = new TurnstileChallengeVerifier(
                new TurnstileConfig(" ", endpoint), secrets, Resilience.noOp()).verify(submission("a-token"));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.PASSED);
        assertThat(violations("hostnameChecked")).isEqualTo(1);
    }

    @Test
    void serverError_thenSuccess_passesOnTheSecondAttempt_withTheSameIdempotencyKey() {
        respond(500, "oops");
        respond(200, PASS_BODY);

        ChallengeVerdict verdict = verifier(TestResilience.retrying(2)).verify(submission("a-token"));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.PASSED);
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).get("idempotency_key")).isEqualTo(requests.get(1).get("idempotency_key"));
    }

    @Test
    void separateVerifications_useSeparateIdempotencyKeys() {
        respond(200, PASS_BODY);
        TurnstileChallengeVerifier verifier = verifier(Resilience.noOp());

        verifier.verify(submission("a-token"));
        verifier.verify(submission("b-token"));

        assertThat(requests.get(0).get("idempotency_key")).isNotEqualTo(requests.get(1).get("idempotency_key"));
    }

    @Test
    void vendorInternalError_isRetried_thenUnavailable() {
        respond(200, "{\"success\":false,\"error-codes\":[\"internal-error\"]}");

        ChallengeVerdict verdict = verifier(TestResilience.retrying(2)).verify(submission("a-token"));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.UNAVAILABLE);
        assertThat(requests).hasSize(2);
        assertThat(violations("serviceAvailable")).isEqualTo(1);
        assertThat(violations("challengePassed")).isZero();
    }

    @Test
    void unparseableBody_isUnavailable_notAPass() {
        respond(200, "<html>a proxy said hello</html>");

        ChallengeVerdict verdict = verifier(Resilience.noOp()).verify(submission("a-token"));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.UNAVAILABLE);
    }

    @Test
    void unreachableEndpoint_isUnavailable() {
        respond(200, PASS_BODY);
        server.stop(0);

        ChallengeVerdict verdict = verifier(Resilience.noOp()).verify(submission("a-token"));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.UNAVAILABLE);
    }

    @Test
    void hungEndpoint_isUnavailableWithinTheOverallCeiling() {
        script.add(new StubResponse(200, PASS_BODY, Duration.ofSeconds(3)));
        TurnstileChallengeVerifier verifier = verifier(TestResilience.bounded(Duration.ofSeconds(2), 2));

        long started = System.nanoTime();
        ChallengeVerdict verdict = verifier.verify(submission("a-token"));
        Duration took = Duration.ofNanos(System.nanoTime() - started);

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.UNAVAILABLE);
        assertThat(took).isLessThan(Duration.ofMillis(2500));
    }

    @Test
    void repeatedFailures_openTheBreaker_andThenNoRequestIsSent() {
        respond(500, "oops");
        TurnstileChallengeVerifier verifier = verifier(TestResilience.breakingAfter(5));
        for (int i = 0; i < 5; i++) {
            assertThat(verifier.verify(submission("a-token")).outcome()).isEqualTo(ChallengeVerdict.Outcome.UNAVAILABLE);
        }
        assertThat(requests).hasSize(5);

        ChallengeVerdict verdict = verifier.verify(submission("a-token"));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.UNAVAILABLE);
        assertThat(requests).hasSize(5);
    }

    @Test
    void definitiveAnswers_doNotCountAgainstTheBreaker() {
        respond(200, "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}");
        TurnstileChallengeVerifier verifier = verifier(TestResilience.breakingAfter(5));

        for (int i = 0; i < 6; i++) {
            verifier.verify(submission("a-token"));
        }

        assertThat(requests).hasSize(6);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankToken_failsWithoutARequest(String token) {
        respond(200, PASS_BODY);

        ChallengeVerdict verdict = verifier(Resilience.noOp()).verify(submission(token));

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.FAILED);
        assertThat(verdict.reasons()).containsExactly("missing-token");
        assertThat(requests).isEmpty();
    }

    @Test
    void missingToken_failsWithoutARequest() {
        respond(200, PASS_BODY);

        ChallengeVerdict verdict = verifier(Resilience.noOp()).verify(submission(null));

        assertThat(verdict.reasons()).containsExactly("missing-token");
        assertThat(requests).isEmpty();
    }

    @Test
    void invalidSubmission_isRefusedWithoutARequest() {
        respond(200, PASS_BODY);
        TurnstileChallengeVerifier verifier = verifier(Resilience.noOp());

        assertThatThrownBy(() -> verifier.verify(null)).isInstanceOf(InvariantViolationException.class);
        assertThat(requests).isEmpty();
    }

    @Test
    void missingCredential_refusesConstruction() {
        SecretProvider none = new EnvironmentSecretProvider(Map.of());

        assertThatThrownBy(() -> new TurnstileChallengeVerifier(TurnstileConfig.of(HOSTNAME), none, Resilience.noOp()))
                .hasMessageContaining("challenge-credential");
    }

    private TurnstileChallengeVerifier verifier(Resilience resilience) {
        return new TurnstileChallengeVerifier(new TurnstileConfig(HOSTNAME, endpoint), secrets, resilience);
    }

    private static ChallengeSubmission submission(String token) {
        return ChallengeSubmission.of(token, "register", null);
    }

    private void respond(int status, String body) {
        script.add(new StubResponse(status, body, Duration.ZERO));
    }

    private double violations(String decision) {
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                .tag("name", "TurnstileChallengeVerifier.verify." + decision).counter();
        return counter == null ? 0 : counter.count();
    }

    private static Map<String, String> form(String encoded) {
        Map<String, String> fields = new HashMap<>();
        for (String pair : encoded.split("&")) {
            String[] parts = pair.split("=", 2);
            fields.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return fields;
    }

    private static void sleep(Duration delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record StubResponse(int status, String body, Duration delay) {
    }

    @Test
    void nullConstructorArguments_areRefusedAndObserved() {
        assertThatThrownBy(() -> new TurnstileChallengeVerifier(null, null, null))
                .isInstanceOf(InvariantViolationException.class);

        assertThat(constructorViolations("config")).isEqualTo(1);
        assertThat(constructorViolations("secrets")).isEqualTo(1);
        assertThat(constructorViolations("resilience")).isEqualTo(1);
    }

    @Test
    void configWithoutAnEndpoint_isRefusedAndObserved() {
        TurnstileConfig noEndpoint = new TurnstileConfig(HOSTNAME, null);

        assertThatThrownBy(() -> new TurnstileChallengeVerifier(noEndpoint, secrets, Resilience.noOp()))
                .isInstanceOf(InvariantViolationException.class);

        assertThat(constructorViolations("config.endpoint")).isEqualTo(1);
    }

    private double constructorViolations(String argument) {
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                .tag("name", "TurnstileChallengeVerifier.constructor." + argument).counter();
        return counter == null ? 0 : counter.count();
    }
}
