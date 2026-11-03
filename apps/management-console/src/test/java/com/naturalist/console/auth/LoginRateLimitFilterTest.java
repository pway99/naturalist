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
 * {@link LoginRateLimitFilter} over the real Resilience4j adapter with a two-permit
 * {@code auth.login} limiter — no Spring context, no login flow: the question is only whether
 * the third attempt in the window is refused before it reaches the chain, whether other
 * requests are untouched, and whether the refusal is observed.
 */
class LoginRateLimitFilterTest {

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new RateLimiterConfig(LoginRateLimitFilter.STRATEGY, 2, Duration.ofMinutes(1), Duration.ZERO)));
        filter = new LoginRateLimitFilter(resilience);
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void thirdLoginAttemptInTheWindow_is429_andNeverReachesTheChain() throws Exception {
        assertThat(attemptLogin().getStatus()).isEqualTo(200);
        assertThat(attemptLogin().getStatus()).isEqualTo(200);

        MockHttpServletResponse refused = attemptLogin();

        assertThat(refused.getStatus()).isEqualTo(429);
        assertThat(refused.getHeader("Retry-After")).isEqualTo("60");
        assertThat(refused.getContentAsString()).contains("try again");
        assertThat(violations("LoginRateLimitFilter.doFilterInternal.withinRate")).isEqualTo(1);
    }

    @Test
    void requestsOtherThanLoginPost_areNotCounted() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest get = new MockHttpServletRequest("GET", "/login");
            get.setServletPath("/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(get, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
        // Still two permits left for real attempts.
        assertThat(attemptLogin().getStatus()).isEqualTo(200);
        assertThat(attemptLogin().getStatus()).isEqualTo(200);
        assertThat(violations("LoginRateLimitFilter.doFilterInternal.withinRate")).isZero();
    }

    private MockHttpServletResponse attemptLogin() throws Exception {
        MockHttpServletRequest post = new MockHttpServletRequest("POST", "/login");
        post.setServletPath("/login");
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
