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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The {@code auth.register} ceiling end to end, through the real filter chain and
 * {@link RegistrationTransaction}: with the limit lowered to one permit per window, the second
 * sign-up in the window is refused with a {@code 429} and the form re-rendered, before any
 * account is created or email sent, and the refusal is observed as {@code withinRate}.
 *
 * <p>The production {@code ResilienceConfiguration} is left intact; only the {@link Resilience}
 * bean is replaced ({@code @Primary}) with one built from the same configs but a one-permit
 * {@code auth.register}, so {@code ResilienceNameValidator} still finds every declared name.
 */
@SpringBootTest
class RegistrationRateLimitWebMvcTest {

    @TestConfiguration
    static class OnePermitPerWindow {
        @Bean
        @Primary
        Resilience tightResilience(List<ResilienceConfig> configs) {
            List<ResilienceConfig> tightened = Stream.concat(
                    configs.stream().filter(c -> !RegistrationTransaction.RATE_STRATEGY.equals(c.name())),
                    Stream.of(new RateLimiterConfig(
                            RegistrationTransaction.RATE_STRATEGY, 1, Duration.ofMinutes(1), Duration.ZERO)))
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
    void secondRegistrationInTheWindow_is429_reRendersForm_andIsObserved() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "first@example.com").param("password", "a fine passphrase")
                        .param("publicHandle", "first-in").param("givenName", "First"))
                .andExpect(redirectedUrl("/register?registered"));

        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "second@example.com").param("password", "a fine passphrase")
                        .param("publicHandle", "second-in").param("givenName", "Second"))
                .andExpect(status().is(429))
                .andExpect(model().attributeExists("error"));

        assertThat(violations("RegistrationTransaction.register.withinRate")).isEqualTo(1);
    }

    private double violations(String name) {
        Counter counter = meters.find("naturalist.invariant.violation").tag("name", name).counter();
        return counter == null ? 0 : counter.count();
    }
}
