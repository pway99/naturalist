package com.naturalist.console.auth;

import com.naturalist.challenge.ChallengeSubmission;
import com.naturalist.challenge.ChallengeVerdict;
import com.naturalist.challenge.ChallengeVerifier;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login through the real filter chain with a scripted {@link ChallengeVerifier}: the password is
 * checked only after a {@code PASSED} verdict, and an unavailable service fails closed (ADR-027 D6,
 * D7). "The password was never checked" is read from the authentication observers, which count
 * every attempt that reaches them.
 */
@SpringBootTest(properties = "naturalist.challenge.site-key=a-site-key")
class LoginChallengeWebMvcTest {

    private static final String BAD_CREDENTIALS = "AuthenticationEventObserver.onApplicationEvent.badCredentials";
    private static final String ACCOUNT_KNOWN = "NaturalistUserDetailsService.loadUserByUsername.accountKnown";
    private static final AtomicInteger NEXT_CLIENT = new AtomicInteger(1);

    @TestConfiguration
    static class ScriptedChallenge {
        @Bean
        @Primary
        ScriptedVerifier scriptedLoginVerifier() {
            return new ScriptedVerifier();
        }
    }

    static final class ScriptedVerifier implements ChallengeVerifier {
        final List<ChallengeSubmission> submissions = new CopyOnWriteArrayList<>();
        volatile ChallengeVerdict verdict = ChallengeVerdict.passed();

        @Override
        public ChallengeVerdict verify(ChallengeSubmission submission) {
            submissions.add(submission);
            return verdict;
        }
    }

    @Autowired
    ScriptedVerifier verifier;

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private MockMvc mockMvc;
    private String client;

    @BeforeEach
    void setUp(@Autowired WebApplicationContext context) {
        client = "198.51.100." + NEXT_CLIENT.getAndIncrement();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())
                .defaultRequest(get("/").with(request -> {
                    request.setRemoteAddr(client);
                    return request;
                }))
                .build();
        verifier.submissions.clear();
        verifier.verdict = ChallengeVerdict.passed();
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void passed_logsIn_andTheVerifierSeesTheTokenActionAndAddress() throws Exception {
        login("test-password", "a-token")
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername("test-admin"));

        assertThat(verifier.submissions).singleElement().satisfies(submission -> {
            assertThat(submission.token()).isEqualTo("a-token");
            assertThat(submission.action()).isEqualTo("login");
            assertThat(submission.clientAddress()).isEqualTo(client);
        });
    }

    @Test
    void passed_withAWrongPassword_stillFailsAsBadCredentials() throws Exception {
        login("guess", "a-token")
                .andExpect(header().string("Location", "/login?error"))
                .andExpect(unauthenticated());

        assertThat(violations(BAD_CREDENTIALS)).isEqualTo(1);
    }

    @Test
    void failed_redirectsToTheChallengeMessage_withoutCheckingThePassword() throws Exception {
        verifier.verdict = ChallengeVerdict.failed("invalid-input-response");

        login("test-password", "a-token")
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/login?challenge"))
                .andExpect(unauthenticated());

        assertThat(violations(BAD_CREDENTIALS)).isZero();
    }

    @Test
    void failed_withAnUnknownEmail_neverReachesTheAccountLookup() throws Exception {
        verifier.verdict = ChallengeVerdict.failed("invalid-input-response");

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "nobody@example.org").param("password", "guess")
                        .param("cf-turnstile-response", "a-token"))
                .andExpect(header().string("Location", "/login?challenge"));

        assertThat(violations(ACCOUNT_KNOWN)).isZero();
    }

    @Test
    void unavailable_failsClosed_withoutCheckingThePassword() throws Exception {
        verifier.verdict = ChallengeVerdict.unavailable("timeout");

        login("test-password", "a-token")
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/login?unavailable"))
                .andExpect(unauthenticated());

        assertThat(violations(BAD_CREDENTIALS)).isZero();
    }

    @Test
    void anOverlongToken_isAFailedChallenge_withoutReachingTheVerifier() throws Exception {
        login("test-password", "x".repeat(ChallengeSubmission.MAX_TOKEN_LENGTH + 1))
                .andExpect(header().string("Location", "/login?challenge"))
                .andExpect(unauthenticated());

        assertThat(verifier.submissions).isEmpty();
    }

    @Test
    void aMissingCsrfToken_isRefusedBeforeTheVerifier() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "test-admin").param("password", "test-password")
                        .param("cf-turnstile-response", "a-token"))
                .andExpect(status().isForbidden());

        assertThat(verifier.submissions).isEmpty();
    }

    @Test
    void otherRequests_doNotReachTheVerifier() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());

        assertThat(verifier.submissions).isEmpty();
    }

    @Test
    void theLoginPage_rendersTheWidget_withTheSiteKeyAndAction() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(containsString("data-sitekey=\"a-site-key\"")))
                .andExpect(content().string(containsString("data-action=\"login\"")))
                .andExpect(content().string(containsString("https://challenges.cloudflare.com/turnstile/v0/api.js")));
    }

    @Test
    void theLoginPage_saysWhatTheChallengeRedirectMeans() throws Exception {
        mockMvc.perform(get("/login").param("challenge", ""))
                .andExpect(content().string(containsString("Please complete the verification and try again.")));
        mockMvc.perform(get("/login").param("unavailable", ""))
                .andExpect(content().string(containsString("We couldn't confirm your browser just now")));
    }

    @Test
    void theLoginPage_showsNoChallengeMessage_byDefault() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(not(containsString("Please complete the verification"))))
                .andExpect(content().string(not(containsString("We couldn't confirm your browser"))));
    }

    private ResultActions login(String password, String token) throws Exception {
        return mockMvc.perform(post("/login").with(csrf())
                .param("username", "test-admin").param("password", password)
                .param("cf-turnstile-response", token));
    }

    private double violations(String name) {
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                .tag("name", name).counter();
        return counter == null ? 0 : counter.count();
    }
}
