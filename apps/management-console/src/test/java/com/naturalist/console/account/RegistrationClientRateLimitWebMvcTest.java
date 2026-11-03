package com.naturalist.console.account;

import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.ResilienceConfig;
import com.naturalist.resilience.ResilienceConfig.RateLimiterConfig;
import com.naturalist.resilience.resilience4j.Resilience4jResilience;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The {@code auth.register.client} ceiling end to end, through the real filter chain and
 * {@link RegistrationTransaction}: with one sign-up per client per window, a second sign-up
 * from the same remote address is refused with a {@code 429} and the form re-rendered, and is
 * observed as {@code withinClientRate} — while a sign-up from another address is still
 * admitted, because the refusal spent none of the system-wide budget.
 *
 * <p>Like {@link RegistrationRateLimitWebMvcTest}, only the {@link Resilience} bean is replaced
 * ({@code @Primary}), from the production configs with {@code auth.register.client} lowered to
 * one permit, so {@code ResilienceNameValidator} still finds every declared name.
 */
@SpringBootTest
class RegistrationClientRateLimitWebMvcTest {

    @TestConfiguration
    static class OnePermitPerClient {
        @Bean
        @Primary
        Resilience tightResilience(List<ResilienceConfig> configs) {
            List<ResilienceConfig> tightened = Stream.concat(
                    configs.stream().filter(c -> !RegistrationTransaction.CLIENT_RATE_STRATEGY.equals(c.name())),
                    Stream.of(new RateLimiterConfig(
                            RegistrationTransaction.CLIENT_RATE_STRATEGY, 1, Duration.ofMinutes(1), Duration.ZERO)))
                    .toList();
            return new Resilience4jResilience(tightened);
        }
    }

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(@Autowired WebApplicationContext context) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void secondRegistrationFromOneClient_is429_whileAnotherClientIsAdmitted() throws Exception {
        mockMvc.perform(register("203.0.113.7", "client-first@example.com", "client-first"))
                .andExpect(redirectedUrl("/register?registered"));

        mockMvc.perform(register("203.0.113.7", "client-second@example.com", "client-second"))
                .andExpect(status().is(429))
                .andExpect(model().attributeExists("error"));

        mockMvc.perform(register("203.0.113.8", "client-other@example.com", "client-other"))
                .andExpect(redirectedUrl("/register?registered"));

        assertThat(violations("RegistrationTransaction.register.withinClientRate")).isEqualTo(1);
        assertThat(violations("RegistrationTransaction.register.withinRate")).isZero();
    }

    private static MockHttpServletRequestBuilder register(String client, String email, String handle) {
        return post("/register").with(csrf())
                .with(request -> {
                    request.setRemoteAddr(client);
                    return request;
                })
                .param("email", email).param("password", "a fine passphrase")
                .param("publicHandle", handle).param("givenName", "Client");
    }

    private double violations(String name) {
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                .tag("name", name).counter();
        return counter == null ? 0 : counter.count();
    }

    /**
     * The client key is personal data: a per-client refusal is recorded by the observer's
     * {@code withinClientRate} count and nowhere else, so the controller's logger receives nothing.
     */
    @Test
    void clientRefusal_writesNoLogRecord() throws Exception {
        Logger logger = Logger.getLogger(RegistrationController.class.getName());
        List<LogRecord> records = new CopyOnWriteArrayList<>();
        Handler capture = new Handler() {
            @Override public void publish(java.util.logging.LogRecord record) { records.add(record); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(capture);
        try {
            mockMvc.perform(register("203.0.113.9", "quiet-first@example.com", "quiet-first"))
                    .andExpect(redirectedUrl("/register?registered"));
            mockMvc.perform(register("203.0.113.9", "quiet-second@example.com", "quiet-second"))
                    .andExpect(status().is(429));

            assertThat(records).extracting(LogRecord::getMessage).isEmpty();
        } finally {
            logger.removeHandler(capture);
        }
    }
}
