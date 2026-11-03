package com.naturalist.console.account;

import com.naturalist.account.AccountQuery;
import com.naturalist.challenge.ChallengeSubmission;
import com.naturalist.challenge.ChallengeVerdict;
import com.naturalist.challenge.ChallengeVerifier;
import com.naturalist.notification.EmailMessage;
import com.naturalist.notification.EmailSender;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registration through the real filter chain with a scripted {@link ChallengeVerifier}: only a
 * {@code PASSED} verdict creates an account or sends mail, and an unavailable service fails closed
 * (ADR-027 D6, D7).
 */
@SpringBootTest(properties = "naturalist.challenge.site-key=a-site-key")
class RegistrationChallengeWebMvcTest {

    private static final AtomicInteger NEXT_CLIENT = new AtomicInteger(1);

    @TestConfiguration
    static class ScriptedChallenge {
        @Bean
        @Primary
        ScriptedVerifier scriptedVerifier() {
            return new ScriptedVerifier();
        }

        @Bean
        @Primary
        RecordingEmailSender recordingChallengeEmailSender() {
            return new RecordingEmailSender();
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

    static final class RecordingEmailSender implements EmailSender {
        final List<EmailMessage> sent = new CopyOnWriteArrayList<>();

        @Override
        public void send(EmailMessage message) {
            sent.add(message);
        }
    }

    @Autowired
    ScriptedVerifier verifier;
    @Autowired
    RecordingEmailSender emailSender;
    @Autowired
    AccountQuery accountQuery;

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
        emailSender.sent.clear();
    }

    @Test
    void passed_registers_andTheVerifierSeesTheTokenActionAndAddress() throws Exception {
        register("passed@example.com", "passed-handle", "a-token")
                .andExpect(redirectedUrl("/register?registered"));

        assertThat(accountQuery.getByEmail("passed@example.com")).isPresent();
        assertThat(emailSender.sent).hasSize(1);
        assertThat(verifier.submissions).singleElement().satisfies(submission -> {
            assertThat(submission.token()).isEqualTo("a-token");
            assertThat(submission.action()).isEqualTo("register");
            assertThat(submission.clientAddress()).isEqualTo(client);
        });
    }

    @Test
    void failed_reRendersWithTheMessage_noAccount_noMail() throws Exception {
        verifier.verdict = ChallengeVerdict.failed("invalid-input-response");

        register("failed@example.com", "failed-handle", "a-token")
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Please complete the verification and try again."))
                .andExpect(model().attribute("email", "failed@example.com"));

        assertThat(accountQuery.getByEmail("failed@example.com")).isEmpty();
        assertThat(emailSender.sent).isEmpty();
    }

    @Test
    void unavailable_failsClosed_with503AndRetryAfter_noAccount_noMail() throws Exception {
        verifier.verdict = ChallengeVerdict.unavailable("timeout");

        register("unavailable@example.com", "unavailable-handle", "a-token")
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(model().attribute("error",
                        "We couldn't confirm your browser just now — please try again in a moment."));

        assertThat(accountQuery.getByEmail("unavailable@example.com")).isEmpty();
        assertThat(emailSender.sent).isEmpty();
    }

    @Test
    void anOverlongToken_isAFailedChallenge_withoutReachingTheVerifier() throws Exception {
        String overlong = "x".repeat(ChallengeSubmission.MAX_TOKEN_LENGTH + 1);

        register("overlong@example.com", "overlong-handle", overlong)
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Please complete the verification and try again."));

        assertThat(verifier.submissions).isEmpty();
        assertThat(accountQuery.getByEmail("overlong@example.com")).isEmpty();
    }

    @Test
    void theLongestLegalToken_stillReachesTheVerifier() throws Exception {
        register("longest@example.com", "longest-handle", "x".repeat(ChallengeSubmission.MAX_TOKEN_LENGTH))
                .andExpect(redirectedUrl("/register?registered"));

        assertThat(verifier.submissions).hasSize(1);
    }

    @Test
    void theForm_rendersTheWidget_withTheSiteKeyAndAction() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-sitekey=\"a-site-key\"")))
                .andExpect(content().string(containsString("data-action=\"register\"")))
                .andExpect(content().string(containsString("https://challenges.cloudflare.com/turnstile/v0/api.js")));
    }

    @Test
    void aReRenderedForm_stillCarriesTheWidget() throws Exception {
        verifier.verdict = ChallengeVerdict.failed("timeout-or-duplicate");

        register("rerender@example.com", "rerender-handle", "a-token")
                .andExpect(content().string(containsString("data-sitekey=\"a-site-key\"")));
    }

    private ResultActions register(String email, String handle, String token) throws Exception {
        return mockMvc.perform(post("/register").with(csrf())
                .param("email", email)
                .param("password", "correct horse battery")
                .param("publicHandle", handle)
                .param("givenName", "Ada")
                .param("cf-turnstile-response", token));
    }
}
