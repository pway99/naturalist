package com.naturalist.console.auth;

import com.naturalist.account.AccessLevel;
import com.naturalist.account.Account;
import com.naturalist.account.AccountName;
import com.naturalist.account.AccountQuery;
import com.naturalist.account.AccountStatus;
import com.naturalist.naturalist.EcologicalStage;
import com.naturalist.naturalist.Naturalist;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.naturalist.NaturalistQuery;
import com.naturalist.naturalist.NaturalistRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies naturalist form login (by account email, via {@link NaturalistUserDetailsService})
 * and route authorization alongside the admin. {@link AccountQuery} and {@link NaturalistQuery}
 * are stubbed directly rather than relying on the mock catalog's fixture password, keeping the
 * expected plaintext under this test's own control.
 */
@SpringBootTest
class NaturalistLoginWebMvcTest {

    private static final String EMAIL = "gerald.durrell@oakvista.example";
    private static final String PASSWORD = "durrell-secret";
    private static final AccountName ACCOUNT_NAME = AccountName.create();
    private static final NaturalistName NATURALIST_NAME = NaturalistName.of("patrick-way");

    @Autowired
    WebApplicationContext context;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    AccountQuery accountQuery;

    @MockitoBean
    NaturalistQuery naturalistQuery;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        var naturalist = new Naturalist(NATURALIST_NAME, ACCOUNT_NAME, "Patrick Way", "Patrick",
                null, NaturalistRole.CARETAKER, EcologicalStage.NATURALIST, null);
        when(naturalistQuery.byAccount(eq(ACCOUNT_NAME))).thenReturn(Optional.of(naturalist));
    }

    private void stubAccount(AccessLevel access, AccountStatus status) {
        var account = new Account(ACCOUNT_NAME, EMAIL, passwordEncoder.encode(PASSWORD),
                true, access, status);
        when(accountQuery.getByEmail(eq(EMAIL))).thenReturn(Optional.of(account));
    }

    @Test
    void naturalist_validCredentials_authenticates() throws Exception {
        stubAccount(AccessLevel.BROWSE_ONLY, AccountStatus.ACTIVE);

        mockMvc.perform(formLogin("/login").user(EMAIL).password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/"))
                .andExpect(authenticated().withUsername(EMAIL).withRoles("NATURALIST"));
    }

    @Test
    void naturalist_visionAccount_grantsVisionAuthority() throws Exception {
        stubAccount(AccessLevel.VISION, AccountStatus.ACTIVE);

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
        stubAccount(AccessLevel.BROWSE_ONLY, AccountStatus.ACTIVE);

        mockMvc.perform(formLogin("/login").user(EMAIL).password("wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login?error"));
    }

    @Test
    void unknownUsername_redirectsToError() throws Exception {
        // accountQuery not stubbed for this email — getByEmail() returns Optional.empty()
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
