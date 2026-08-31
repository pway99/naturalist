package com.naturalist.console.account;

import com.naturalist.account.AccessLevel;
import com.naturalist.account.AccountQuery;
import com.naturalist.notification.EmailMessage;
import com.naturalist.notification.EmailSender;
import com.naturalist.naturalist.NaturalistQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack ({@code @SpringBootTest}, real security filter chain) coverage of public
 * self-registration and email verification. Runs on the mock-data profile like the other
 * console {@code @SpringBootTest}s; outbound mail is captured by a recording
 * {@link EmailSender} so the verification token can be read back exactly as a real user
 * would receive it.
 */
@SpringBootTest
class RegistrationFlowWebMvcTest {

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("/verify\\?token=(\\S+)");

    @TestConfiguration
    static class RecordingEmailConfig {
        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
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
    WebApplicationContext context;
    @Autowired
    RecordingEmailSender emailSender;
    @Autowired
    AccountQuery accountQuery;
    @Autowired
    NaturalistQuery naturalistQuery;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        emailSender.sent.clear();
    }

    @Test
    void register_createsAccountAndNaturalist_autoLogsIn_andEmailsLink() throws Exception {
        String email = "ada@example.com";

        mockMvc.perform(post("/register").with(csrf())
                        .param("email", email)
                        .param("password", "correct horse battery")
                        .param("publicHandle", "ada-lovelace")
                        .param("givenName", "Ada")
                        .param("familyName", "Lovelace"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register?registered"))
                .andExpect(authenticated().withUsername(email));

        // Both domain records exist and are linked, without VISION yet (unverified).
        var account = accountQuery.getByEmail(email).orElseThrow();
        assertThat(account.access()).isEqualTo(AccessLevel.BROWSE_ONLY);
        var naturalist = naturalistQuery.byAccount(account.name()).orElseThrow();
        assertThat(naturalist.publicHandle()).isEqualTo("ada-lovelace");
        assertThat(naturalist.givenName()).isEqualTo("Ada");

        // The verification link was emailed.
        assertThat(emailSender.sent).hasSize(1);
        assertThat(emailSender.sent.getFirst().to()).isEqualTo(email);
        assertThat(emailSender.sent.getFirst().body()).contains("/verify?token=");
    }

    @Test
    void register_thenVerify_grantsVisionAndKeepsSessionAuthenticated() throws Exception {
        String email = "grace@example.com";

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/register").with(csrf())
                        .param("email", email)
                        .param("password", "another good passphrase")
                        .param("publicHandle", "grace-hopper")
                        .param("givenName", "Grace"))
                .andExpect(redirectedUrl("/register?registered"))
                .andReturn().getRequest().getSession();

        String token = extractToken(emailSender.sent.getFirst().body());

        // Verify in the same session: self-serve policy grants VISION.
        mockMvc.perform(get("/verify").param("token", token).session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("success", true))
                .andExpect(authenticated().withUsername(email));

        assertThat(accountQuery.getByEmail(email).orElseThrow().access())
                .isEqualTo(AccessLevel.VISION);
    }

    @Test
    void register_duplicateEmail_reRendersFormWithError_notAuthenticated() throws Exception {
        String email = "dup@example.com";
        mockMvc.perform(post("/register").with(csrf())
                        .param("email", email).param("password", "first passphrase here")
                        .param("publicHandle", "first-handle").param("givenName", "First"))
                .andExpect(redirectedUrl("/register?registered"));
        emailSender.sent.clear();

        mockMvc.perform(post("/register").with(csrf())
                        .param("email", email).param("password", "second passphrase here")
                        .param("publicHandle", "second-handle").param("givenName", "Second"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(unauthenticated());

        assertThat(emailSender.sent).isEmpty();
    }

    @Test
    void register_shortPassword_reRendersFormWithError() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "short@example.com").param("password", "tiny")
                        .param("publicHandle", "short-pw").param("givenName", "Shorty"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(unauthenticated());

        assertThat(accountQuery.getByEmail("short@example.com")).isEmpty();
        assertThat(emailSender.sent).isEmpty();
    }

    @Test
    void register_malformedEmail_reRendersFormWithError() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "not-an-email").param("password", "a valid passphrase")
                        .param("publicHandle", "bad-email").param("givenName", "Nomail"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(unauthenticated());
    }

    @Test
    void verify_badToken_showsFailurePage() throws Exception {
        mockMvc.perform(get("/verify").param("token", "not-a-real-token"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("success", false));
    }

    private static String extractToken(String emailBody) {
        Matcher matcher = TOKEN_IN_LINK.matcher(emailBody);
        assertThat(matcher.find()).as("verification link in email body").isTrue();
        return matcher.group(1);
    }
}
