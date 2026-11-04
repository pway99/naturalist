package com.naturalist.console.auth;

import com.naturalist.challenge.ChallengeSubmission;
import com.naturalist.challenge.ChallengeVerdict;
import com.naturalist.challenge.ChallengeVerifier;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.ResilienceConfig;
import com.naturalist.resilience.ResilienceConfig.RateLimiterConfig;
import com.naturalist.resilience.resilience4j.Resilience4jResilience;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The login order (ADR-027 D7): the per-client permit comes before the challenge, so a client over
 * {@code auth.login.client} costs no siteverify round trip. Only the {@link Resilience} bean and
 * the verifier are replaced, with {@code auth.login.client} lowered to one permit.
 */
@SpringBootTest
class LoginChallengeOrderWebMvcTest {

    @TestConfiguration
    static class OnePermitPerClient {
        @Bean
        @Primary
        Resilience tightLoginResilience(List<ResilienceConfig> configs) {
            return new Resilience4jResilience(Stream.concat(
                    configs.stream().filter(c -> !LoginRateLimitFilter.CLIENT_STRATEGY.equals(c.name())),
                    Stream.of(new RateLimiterConfig(
                            LoginRateLimitFilter.CLIENT_STRATEGY, 1, Duration.ofMinutes(1), Duration.ZERO)))
                    .toList());
        }

        @Bean
        @Primary
        CountingVerifier countingLoginVerifier() {
            return new CountingVerifier();
        }
    }

    static final class CountingVerifier implements ChallengeVerifier {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public ChallengeVerdict verify(ChallengeSubmission submission) {
            calls.incrementAndGet();
            return ChallengeVerdict.failed("invalid-input-response");
        }
    }

    @Test
    void aClientOverItsLimit_gets429_andTheVerifierIsNeverCalled(
            @Autowired WebApplicationContext context, @Autowired CountingVerifier verifier) throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        mockMvc.perform(post("/login").with(csrf()).param("username", "someone").param("password", "guess"))
                .andExpect(status().isSeeOther());
        assertThat(verifier.calls).hasValue(1);

        mockMvc.perform(post("/login").with(csrf()).param("username", "someone").param("password", "guess"))
                .andExpect(status().isTooManyRequests());
        assertThat(verifier.calls).hasValue(1);
    }
}
