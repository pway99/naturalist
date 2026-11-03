package com.naturalist.console.auth;

import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.ResilienceConfig.RateLimiterConfig;
import com.naturalist.resilience.resilience4j.Resilience4jResilience;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LoginRateLimitFilter} over the real Resilience4j adapter — no Spring context, no login
 * flow. The global tests give {@code auth.login} two permits and the per-client ceiling room to
 * spare; the per-client tests do the reverse. The questions: is an attempt over either ceiling
 * refused before it reaches the chain, are other requests untouched, is one client's flood
 * confined to that client, and is each refusal observed under its own name.
 */
class LoginRateLimitFilterTest {

    private static final String WITHIN_RATE = "LoginRateLimitFilter.doFilterInternal.withinRate";
    private static final String WITHIN_CLIENT_RATE = "LoginRateLimitFilter.doFilterInternal.withinClientRate";

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void thirdLoginAttemptInTheWindow_is429_andNeverReachesTheChain() throws Exception {
        LoginRateLimitFilter filter = filter(2, 100);
        assertThat(attemptLogin(filter, "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(attemptLogin(filter, "10.0.0.2").getStatus()).isEqualTo(200);

        MockHttpServletResponse refused = attemptLogin(filter, "10.0.0.3");

        assertThat(refused.getStatus()).isEqualTo(429);
        assertThat(refused.getHeader("Retry-After")).isEqualTo("60");
        assertThat(refused.getContentAsString()).contains("try again");
        assertThat(violations(WITHIN_RATE)).isEqualTo(1);
        assertThat(violations(WITHIN_CLIENT_RATE)).isZero();
    }

    @Test
    void requestsOtherThanLoginPost_areNotCounted() throws Exception {
        LoginRateLimitFilter filter = filter(2, 2);
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest get = new MockHttpServletRequest("GET", "/login");
            get.setServletPath("/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(get, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
        // Still two permits left, globally and for this client, for real attempts.
        assertThat(attemptLogin(filter, "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(attemptLogin(filter, "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(violations(WITHIN_RATE)).isZero();
        assertThat(violations(WITHIN_CLIENT_RATE)).isZero();
    }

    @Test
    void oneClientOverItsCeiling_is429_whileAnotherClientIsAdmitted() throws Exception {
        LoginRateLimitFilter filter = filter(100, 2);
        assertThat(attemptLogin(filter, "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(attemptLogin(filter, "10.0.0.1").getStatus()).isEqualTo(200);

        MockHttpServletResponse refused = attemptLogin(filter, "10.0.0.1");

        assertThat(refused.getStatus()).isEqualTo(429);
        assertThat(refused.getHeader("Retry-After")).isEqualTo("60");
        assertThat(attemptLogin(filter, "10.0.0.2").getStatus()).isEqualTo(200);
        assertThat(violations(WITHIN_CLIENT_RATE)).isEqualTo(1);
        // A client refusal is not also a global one: the global permit was never asked for.
        assertThat(violations(WITHIN_RATE)).isZero();
    }

    @Test
    void attemptsRefusedPerClient_doNotSpendTheGlobalBudget() throws Exception {
        LoginRateLimitFilter filter = filter(2, 1);
        assertThat(attemptLogin(filter, "10.0.0.1").getStatus()).isEqualTo(200);
        for (int i = 0; i < 5; i++) {
            assertThat(attemptLogin(filter, "10.0.0.1").getStatus()).isEqualTo(429);
        }

        // The flood took no global permits, so the second of the two is still there for someone else.
        assertThat(attemptLogin(filter, "10.0.0.2").getStatus()).isEqualTo(200);
        assertThat(attemptLogin(filter, "10.0.0.3").getStatus()).isEqualTo(429);
        assertThat(violations(WITHIN_CLIENT_RATE)).isEqualTo(5);
        assertThat(violations(WITHIN_RATE)).isEqualTo(1);
    }

    private static LoginRateLimitFilter filter(int globalPermits, int perClientPermits) {
        Resilience resilience = new Resilience4jResilience(List.of(
                new RateLimiterConfig(LoginRateLimitFilter.STRATEGY, globalPermits, Duration.ofMinutes(1), Duration.ZERO),
                new RateLimiterConfig(LoginRateLimitFilter.CLIENT_STRATEGY, perClientPermits, Duration.ofMinutes(1), Duration.ZERO)));
        return new LoginRateLimitFilter(resilience);
    }

    private static MockHttpServletResponse attemptLogin(LoginRateLimitFilter filter, String client) throws Exception {
        MockHttpServletRequest post = new MockHttpServletRequest("POST", "/login");
        post.setServletPath("/login");
        post.setRemoteAddr(client);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(post, response, new MockFilterChain());
        return response;
    }

    private double violations(String name) {
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                .tag("name", name).counter();
        return counter == null ? 0 : counter.count();
    }
}
