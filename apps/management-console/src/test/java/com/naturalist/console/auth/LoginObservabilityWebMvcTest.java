package com.naturalist.console.auth;

import com.naturalist.account.AccountCommand;
import com.naturalist.account.AccountName;
import com.naturalist.account.AccountQuery;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Proves the authentication attack signals reach Micrometer end to end — through the real
 * security filter chain, Boot's {@code DefaultAuthenticationEventPublisher}, the
 * {@link AuthenticationEventObserver} listener and {@link NaturalistUserDetailsService}'s own
 * decision observations. Runs on the mock-data profile like the other console
 * {@code @SpringBootTest}s; a private {@link SimpleMeterRegistry} is bound to the global
 * composite for each test so it reads only the metrics that test caused (ADR-017 rationale).
 */
@SpringBootTest
class LoginObservabilityWebMvcTest {

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private MockMvc mockMvc;

    @Autowired
    private AccountQuery accountQuery;
    @Autowired
    private AccountCommand accountCommand;

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
    void login_unknownEmail_isObservedAsUnknownAccountAndBadCredentials() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "nobody@example.org").param("password", "guess"))
                .andExpect(redirectedUrl("/login?error"));

        // Spring hid "not found" from the caller as bad credentials; the metrics keep the distinction.
        assertThat(violations("NaturalistUserDetailsService.loadUserByUsername.accountKnown")).isEqualTo(1);
        assertThat(violations("AuthenticationEventObserver.onApplicationEvent.authenticationFailed")).isEqualTo(1);
        assertThat(violations("AuthenticationEventObserver.onApplicationEvent.badCredentials")).isEqualTo(1);
        assertThat(violations("AuthenticationEventObserver.onApplicationEvent.suspendedAccountAttempt")).isZero();
        assertThat(violations("AuthenticationEventObserver.onApplicationEvent.adminAuthenticationFailed")).isZero();
    }

    @Test
    void login_adminWrongPassword_isObservedAsAdminAuthenticationFailed() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "test-admin").param("password", "guess"))
                .andExpect(redirectedUrl("/login?error"));

        // The one credential whose guessing rate deserves its own alert.
        assertThat(violations("AuthenticationEventObserver.onApplicationEvent.adminAuthenticationFailed")).isEqualTo(1);
        assertThat(violations("AuthenticationEventObserver.onApplicationEvent.badCredentials")).isEqualTo(1);
    }

    @Test
    void login_knownEmailWrongPassword_isObservedAsBadCredentialsOnly() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "gerald.durrell@oakvista.example").param("password", "guess"))
                .andExpect(redirectedUrl("/login?error"));

        // The email exists (seeded), so this is password guessing, not enumeration.
        assertThat(violations("NaturalistUserDetailsService.loadUserByUsername.accountKnown")).isZero();
        assertThat(violations("AuthenticationEventObserver.onApplicationEvent.badCredentials")).isEqualTo(1);
    }

    @Test
    void login_suspendedAccount_isObservedAsSuspendedAccountAttempt() throws Exception {
        AccountName gerald = accountQuery.getByEmail("gerald.durrell@oakvista.example").orElseThrow().name();
        accountCommand.suspend(gerald);
        try {
            // The lock check runs before the password check, so even a wrong password surfaces
            // the suspension — exactly the signal wanted: a banned account trying to get back in.
            mockMvc.perform(post("/login").with(csrf())
                            .param("username", "gerald.durrell@oakvista.example").param("password", "guess"))
                    .andExpect(redirectedUrl("/login?error"));

            assertThat(violations("NaturalistUserDetailsService.loadUserByUsername.accountActive")).isEqualTo(1);
            assertThat(violations("AuthenticationEventObserver.onApplicationEvent.suspendedAccountAttempt")).isEqualTo(1);
            assertThat(violations("AuthenticationEventObserver.onApplicationEvent.badCredentials")).isZero();
        } finally {
            accountCommand.reinstate(gerald);   // the seeded account is shared with the other tests
        }
    }

    private double violations(String name) {
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                .tag("name", name).counter();
        return counter == null ? 0 : counter.count();
    }
}
