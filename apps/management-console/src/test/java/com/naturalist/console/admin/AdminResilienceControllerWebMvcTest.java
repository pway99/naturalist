package com.naturalist.console.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end render + auth assertions for {@code /admin/resilience}
 * (admin-console plan view 1, M4).
 *
 * <p>Anchors the acceptance criteria from
 * {@code docs/plans/admin-console.md}: an ADMIN-authenticated GET renders
 * a 200 page that lists every currently registered strategy name under
 * its correct primitive heading, and an anonymous GET is bounced to the
 * login page by the existing security chain.
 *
 * <p>The registered set comes from
 * {@link com.naturalist.console.resilience.ResilienceConfiguration} —
 * {@code catalog.fanout} (timeout + circuit breaker),
 * {@code image.conversion} (timeout only), and
 * {@code vision.identification} (timeout + rate limiter). Retry and
 * bulkhead are unused today and render the empty state.
 */
@SpringBootTest
class AdminResilienceControllerWebMvcTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void anonymous_resilience_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/resilience"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login"));
    }

    @Test
    void admin_resilience_rendersOk() throws Exception {
        mockMvc.perform(get("/admin/resilience").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void admin_resilience_listsEachStrategyUnderItsPrimitive() throws Exception {
        var html = mockMvc.perform(get("/admin/resilience")
                        .with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var retryAt = html.indexOf(">Retry</h2>");
        var timeoutAt = html.indexOf(">Timeout</h2>");
        var breakerAt = html.indexOf(">Circuit Breaker</h2>");
        var bulkheadAt = html.indexOf(">Bulkhead</h2>");
        var rateLimiterAt = html.indexOf(">Rate Limiter</h2>");

        assertThat(retryAt).as("Retry heading").isGreaterThan(-1);
        assertThat(timeoutAt).as("Timeout heading").isGreaterThan(retryAt);
        assertThat(breakerAt).as("Circuit Breaker heading").isGreaterThan(timeoutAt);
        assertThat(bulkheadAt).as("Bulkhead heading").isGreaterThan(breakerAt);
        assertThat(rateLimiterAt).as("Rate Limiter heading").isGreaterThan(bulkheadAt);

        var retrySection = html.substring(retryAt, timeoutAt);
        var timeoutSection = html.substring(timeoutAt, breakerAt);
        var breakerSection = html.substring(breakerAt, bulkheadAt);
        var bulkheadSection = html.substring(bulkheadAt, rateLimiterAt);
        var rateLimiterSection = html.substring(rateLimiterAt);

        assertThat(retrySection).contains("None registered.");
        assertThat(timeoutSection)
                .contains("catalog.fanout")
                .contains("image.conversion")
                .contains("vision.identification");
        assertThat(breakerSection)
                .contains("catalog.fanout")
                .doesNotContain("image.conversion");
        assertThat(bulkheadSection).contains("None registered.");
        assertThat(rateLimiterSection).contains("vision.identification");
    }
}
