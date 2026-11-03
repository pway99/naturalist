package com.naturalist.console.resilience;

import com.naturalist.challenge.ChallengeSubmission;
import com.naturalist.challenge.ChallengeVerdict;
import com.naturalist.challenge.turnstile.TurnstileChallengeVerifier;
import com.naturalist.challenge.turnstile.TurnstileConfig;
import com.naturalist.resilience.ResilienceConfig;
import com.naturalist.resilience.resilience4j.Resilience4jResilience;
import com.naturalist.secrets.EnvironmentSecretProvider;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The adapter on the production {@code challenge.verify} strategy through real Resilience4j, whose
 * timeout and open-state exceptions the adapter module's own double only imitates. An adapter may
 * not depend on another adapter, so this can only live at the composition root.
 */
class ChallengeVerifyStrategyTest {

    private final AtomicInteger hits = new AtomicInteger();
    private volatile int status = 200;
    private volatile String body = "{\"success\":true,\"action\":\"register\"}";
    private volatile Duration delay = Duration.ZERO;

    private HttpServer server;
    private TurnstileChallengeVerifier verifier;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/siteverify", exchange -> {
            hits.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        ResilienceConfiguration production = new ResilienceConfiguration();
        List<ResilienceConfig> strategy = List.of(
                production.challengeVerifyTimeout(),
                production.challengeVerifyRetry(),
                production.challengeVerifyCircuitBreaker());
        verifier = new TurnstileChallengeVerifier(
                new TurnstileConfig(null, URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify")),
                new EnvironmentSecretProvider(Map.of("CHALLENGE_CREDENTIAL", "not-a-real-secret")),
                new Resilience4jResilience(strategy));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void aDefinitiveAnswer_passesThroughTheStrategy() {
        assertThat(verify().outcome()).isEqualTo(ChallengeVerdict.Outcome.PASSED);
        assertThat(hits).hasValue(1);
    }

    @Test
    void aHungVendor_comesBackUnavailable_withinTheTwoSecondCeiling() {
        delay = Duration.ofSeconds(10);

        long started = System.nanoTime();
        ChallengeVerdict verdict = verify();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        assertThat(verdict.outcome()).isEqualTo(ChallengeVerdict.Outcome.UNAVAILABLE);
        assertThat(elapsed).isLessThan(Duration.ofMillis(2500));
    }

    @Test
    void aFailingVendor_opensTheBreaker_andLaterCallsStopReachingIt() {
        status = 503;
        for (int i = 0; i < 5; i++) {
            assertThat(verify().outcome()).isEqualTo(ChallengeVerdict.Outcome.UNAVAILABLE);
        }
        int hitsWhenOpen = hits.get();

        assertThat(verify().outcome()).isEqualTo(ChallengeVerdict.Outcome.UNAVAILABLE);

        assertThat(hits).hasValue(hitsWhenOpen);
    }

    @Test
    void rejectedTokens_neverOpenTheBreaker() {
        body = "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}";
        for (int i = 0; i < 6; i++) {
            assertThat(verify().outcome()).isEqualTo(ChallengeVerdict.Outcome.FAILED);
        }

        assertThat(hits).hasValue(6);
    }

    private ChallengeVerdict verify() {
        return verifier.verify(ChallengeSubmission.of("a-token", "register", null));
    }
}
