package com.naturalist.console.auth;

import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.ResilienceConfig;
import com.naturalist.resilience.ResilienceConfig.RateLimiterConfig;
import com.naturalist.resilience.resilience4j.Resilience4jResilience;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The {@code vision.identification.client} ceiling end to end, through the real security chain
 * rather than a hand-built request: {@link IdentifyRateLimitFilter} must match
 * {@code POST /insects/identify} as MockMvc and the container both present it, and sits after
 * authorization, so a request that is refused sign-in or {@code VISION} spends no permit.
 *
 * <p>The refusal is a redirect to the identify page with {@code ?limit=RATE}, because the
 * upload script submits by {@code fetch} and follows a redirect but silently reloads on any
 * other answer. Only the {@link Resilience} bean is replaced ({@code @Primary}), with the
 * per-client ceiling lowered to one permit, so {@code ResilienceNameValidator} still finds every
 * declared name. Each test uses its own documentation-range address, because the permits are
 * held by the shared context.
 */
@SpringBootTest
class IdentifyClientRateLimitWebMvcTest {

    /** Minimal JPEG header: the controller is reached, not exercised. */
    private static final byte[] TINY_JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    @TestConfiguration
    static class OnePermitPerClient {
        @Bean
        @Primary
        Resilience tightResilience(List<ResilienceConfig> configs) {
            return new Resilience4jResilience(Stream.concat(
                    configs.stream().filter(c -> !IdentifyRateLimitFilter.STRATEGY.equals(c.name())),
                    Stream.of(new RateLimiterConfig(
                            IdentifyRateLimitFilter.STRATEGY, 1, Duration.ofMinutes(1), Duration.ZERO)))
                    .toList());
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(@Autowired WebApplicationContext context) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void secondIdentifyFromOneClient_isRedirectedToTheRateNotice() throws Exception {
        mockMvc.perform(identify("198.51.100.7").with(vision()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(identify("198.51.100.7").with(vision()))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl("/insects/identify?limit=RATE"));
    }

    @Test
    void requestsRefusedByAuthorization_spendNoPermit() throws Exception {
        mockMvc.perform(identify("198.51.100.8"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(identify("198.51.100.8").with(user("naturalist")
                        .authorities(new SimpleGrantedAuthority("ROLE_NATURALIST"))))
                .andExpect(status().isForbidden());

        // The one permit is still unspent, so this reaches the controller (not the notice).
        mockMvc.perform(identify("198.51.100.8").with(vision()))
                .andExpect(status().isFound());
    }

    private static MockMultipartHttpServletRequestBuilder identify(String client) {
        return multipart("/insects/identify")
                .file(new MockMultipartFile("image", "test.jpg", "image/jpeg", TINY_JPEG))
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr(client);
                    return request;
                });
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor vision() {
        return user("naturalist").authorities(
                new SimpleGrantedAuthority("ROLE_NATURALIST"), new SimpleGrantedAuthority("VISION"));
    }
}
