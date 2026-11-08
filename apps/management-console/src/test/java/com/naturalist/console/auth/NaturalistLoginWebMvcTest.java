package com.naturalist.console.auth;

import com.naturalist.account.AccessLevel;
import com.naturalist.account.Account;
import com.naturalist.account.AccountStatus;
import com.naturalist.account.AccountTestEntitySource;
import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies naturalist form login (by account email, via {@link NaturalistUserDetailsService})
 * and route authorization alongside the admin. The app's real {@code AccountQuery} and
 * {@code NaturalistQuery} run over the mock catalog: the fixture account
 * {@value #EMAIL} is linked to the fixture naturalist {@code patrick-way}. Each test rewrites
 * that account's password hash, access and status in the shared database, keeping the expected
 * plaintext under this test's own control, and restores the fixture account afterwards.
 */
@SpringBootTest
class NaturalistLoginWebMvcTest {

    private static final String EMAIL = "gerald.durrell@oakvista.example";
    private static final String PASSWORD = "durrell-secret";

    @Autowired
    WebApplicationContext context;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    NaturalistDatabase database;

    MockMvc mockMvc;

    /** The fixture account as the catalog holds it, restored after each test. */
    private Account fixtureAccount;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        fixtureAccount = database.getNamed(AccountTestEntitySource.class).entityStream()
                .filter(a -> a.email().equals(EMAIL))
                .findFirst()
                .orElseThrow();
    }

    @AfterEach
    void restoreFixtureAccount() {
        database.getNamed(AccountTestEntitySource.class).update(fixtureAccount);
    }

    private void withAccount(AccessLevel access, AccountStatus status) {
        database.getNamed(AccountTestEntitySource.class).update(new Account(fixtureAccount.name(), EMAIL,
                passwordEncoder.encode(PASSWORD), true, access, status));
    }

    @Test
    void naturalist_validCredentials_authenticates() throws Exception {
        withAccount(AccessLevel.BROWSE_ONLY, AccountStatus.ACTIVE);

        mockMvc.perform(formLogin("/login").user(EMAIL).password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/"))
                .andExpect(authenticated().withUsername(EMAIL).withRoles("NATURALIST"));
    }

    @Test
    void naturalist_visionAccount_grantsVisionAuthority() throws Exception {
        withAccount(AccessLevel.VISION, AccountStatus.ACTIVE);

        // withAuthentication (not withAuthorities, which requires an exact-set match) because
        // Spring Security 6.5+ also attaches a FactorGrantedAuthority (FACTOR_PASSWORD) tracking
        // which authentication factor was used — unrelated to the VISION entitlement under test.
        mockMvc.perform(formLogin("/login").user(EMAIL).password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withAuthentication(auth -> {
                    var authorityNames = auth.getAuthorities().stream()
                            .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                            .toList();
                    assertThat(authorityNames).contains("ROLE_NATURALIST", "VISION");
                }));
    }

    @Test
    void naturalist_wrongPassword_redirectsToError() throws Exception {
        withAccount(AccessLevel.BROWSE_ONLY, AccountStatus.ACTIVE);

        mockMvc.perform(formLogin("/login").user(EMAIL).password("wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login?error"));
    }

    @Test
    void unknownUsername_redirectsToError() throws Exception {
        // no account in the catalog has this email
        mockMvc.perform(formLogin("/login").user("no-such-naturalist@oakvista.example").password("x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login?error"));
    }

    @Test
    void naturalist_forbiddenFromAdminRoute() throws Exception {
        mockMvc.perform(get("/admin/anything").with(user("patrick-way").roles("NATURALIST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_validCredentials_authenticatesWithAdminRole() throws Exception {
        mockMvc.perform(formLogin("/login").user("test-admin").password("test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/"))
                .andExpect(authenticated().withUsername("test-admin").withRoles("ADMIN"));
    }
}
