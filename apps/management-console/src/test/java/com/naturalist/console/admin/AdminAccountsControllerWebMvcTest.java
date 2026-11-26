package com.naturalist.console.admin;

import com.naturalist.account.AccessLevel;
import com.naturalist.account.Account;
import com.naturalist.account.AccountQuery;
import com.naturalist.account.AccountStatus;
import com.naturalist.account.AccountTestEntitySource;
import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack ({@code @SpringBootTest}, real security chain) coverage of {@code /admin/accounts}
 * (launch item 1.9), in the style of the other admin page tests. The app's real
 * {@code AccountQuery} and {@code AccountCommand} run over the mock catalog; every test that
 * changes a fixture account restores the whole account fixture afterwards, since the
 * {@code @SpringBootTest} context and its database are shared across test classes.
 */
@SpringBootTest
class AdminAccountsControllerWebMvcTest {

    private static final String DURRELL = "gerald.durrell@oakvista.example";
    private static final String DURRELL_PASSWORD = "durrell-secret";
    private static final String PENDING = "pending.visitor@oakvista.example";

    @Autowired
    WebApplicationContext context;

    @Autowired
    AccountQuery accountQuery;

    @Autowired
    NaturalistDatabase database;

    @Autowired
    PasswordEncoder passwordEncoder;

    MockMvc mockMvc;

    private List<Account> fixtureAccounts;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        fixtureAccounts = database.getNamed(AccountTestEntitySource.class).entityStream().toList();
    }

    @AfterEach
    void restoreFixtureAccounts() {
        AccountTestEntitySource source = database.getNamed(AccountTestEntitySource.class);
        for (Account account : fixtureAccounts) {
            source.update(account);
        }
    }

    private Account account(String email) {
        return accountQuery.getByEmail(email).orElseThrow();
    }

    @Test
    void anonymous_accounts_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/accounts"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login"));
    }

    @Test
    @WithMockUser(roles = "NATURALIST")
    void naturalist_accounts_isForbidden() throws Exception {
        mockMvc.perform(get("/admin/accounts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "NATURALIST")
    void naturalist_cannotSuspend() throws Exception {
        String name = account(DURRELL).name().value();
        mockMvc.perform(post("/admin/accounts/" + name + "/suspend").with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(account(DURRELL).status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void admin_accounts_listsEachAccountWithItsState() throws Exception {
        String html = mockMvc.perform(get("/admin/accounts").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(DURRELL, PENDING, "young-naturalist@oakvista.example");
        int pendingAt = html.indexOf(PENDING);
        String pendingRow = html.substring(pendingAt, html.indexOf("</tr>", pendingAt));
        assertThat(pendingRow)
                .contains("<td>yes</td>", "browse only", "active")
                .contains("/admin/accounts/" + account(PENDING).name().value() + "/grant-vision")
                .contains("/suspend")
                .contains("name=\"_csrf\"");
    }

    @Test
    void admin_grantThenRevokeVision_changesAccess() throws Exception {
        String name = account(PENDING).name().value();

        mockMvc.perform(post("/admin/accounts/" + name + "/grant-vision").with(csrf())
                .param("page", "0")
                .with(user("test-admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/accounts?page=0"))
                .andExpect(flash().attribute("notice", containsString("Granted")));
        assertThat(account(PENDING).access()).isEqualTo(AccessLevel.VISION);

        mockMvc.perform(post("/admin/accounts/" + name + "/revoke-vision").with(csrf())
                .with(user("test-admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/accounts?page=0"))
                .andExpect(flash().attribute("notice", containsString("Revoked")));
        assertThat(account(PENDING).access()).isEqualTo(AccessLevel.BROWSE_ONLY);
    }

    @Test
    void admin_suspendThenReinstate_changesStatus() throws Exception {
        String name = account(PENDING).name().value();

        mockMvc.perform(post("/admin/accounts/" + name + "/suspend").with(csrf())
                .param("page", "2")
                .with(user("test-admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/accounts?page=2"))
                .andExpect(flash().attribute("notice", containsString("Suspended " + PENDING)));
        assertThat(account(PENDING).status()).isEqualTo(AccountStatus.SUSPENDED);

        mockMvc.perform(post("/admin/accounts/" + name + "/reinstate").with(csrf())
                .with(user("test-admin").roles("ADMIN")))
                .andExpect(flash().attribute("notice", containsString("Reinstated")));
        assertThat(account(PENDING).status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void admin_postWithoutCsrf_isForbidden() throws Exception {
        String name = account(PENDING).name().value();
        mockMvc.perform(post("/admin/accounts/" + name + "/grant-vision")
                .with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        assertThat(account(PENDING).access()).isEqualTo(AccessLevel.BROWSE_ONLY);
    }

    @Test
    void admin_unknownAccount_redirectsWithNotice() throws Exception {
        mockMvc.perform(post("/admin/accounts/acct-nobody-here/suspend").with(csrf())
                .with(user("test-admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/accounts?page=0"))
                .andExpect(flash().attribute("notice", containsString("No account")));
    }

    @Test
    void admin_malformedAccountName_redirectsWithNotice() throws Exception {
        mockMvc.perform(post("/admin/accounts/Not_A_Slug/suspend").with(csrf())
                .with(user("test-admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/accounts?page=0"))
                .andExpect(flash().attribute("notice", containsString("not a valid account name")));
    }

    /**
     * Suspend is a ban: a naturalist already signed in is signed out on their next request,
     * every session of theirs, and other accounts' sessions are untouched.
     */
    @Test
    void admin_suspend_endsEveryLiveSessionOfTheAccount() throws Exception {
        AccountTestEntitySource source = database.getNamed(AccountTestEntitySource.class);
        Account durrell = account(DURRELL);
        source.update(durrell.withPasswordHash(passwordEncoder.encode(DURRELL_PASSWORD)));

        MockHttpSession laptop = signIn(DURRELL, DURRELL_PASSWORD, "198.51.100.191");
        MockHttpSession phone = signIn(DURRELL, DURRELL_PASSWORD, "198.51.100.192");
        mockMvc.perform(get("/").session(laptop))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(DURRELL));

        mockMvc.perform(post("/admin/accounts/" + durrell.name().value() + "/suspend").with(csrf())
                .with(user("test-admin").roles("ADMIN")))
                // At least these two; other test classes share this context and its registry.
                .andExpect(flash().attribute("notice", containsString("live sessions.")));

        mockMvc.perform(get("/").session(laptop))
                .andExpect(redirectedUrl("/login?expired"));
        mockMvc.perform(get("/").session(phone))
                .andExpect(redirectedUrl("/login?expired"));
        // And the ban holds: a suspended account cannot sign back in.
        mockMvc.perform(post("/login").with(csrf()).with(fromAddress("198.51.100.193"))
                .param("username", DURRELL).param("password", DURRELL_PASSWORD))
                .andExpect(redirectedUrl("/login?error"));
    }

    /** Registration signs the new naturalist in without the login filter; that session ends too. */
    @Test
    void admin_suspend_endsTheSessionRegistrationSignedIn() throws Exception {
        String email = "suspend-me@example.com";
        MockHttpSession registered = (MockHttpSession) mockMvc.perform(post("/register").with(csrf())
                .with(fromAddress("198.51.100.190"))
                .param("email", email)
                .param("password", "a long enough passphrase")
                .param("publicHandle", "suspend-me")
                .param("givenName", "Suspended"))
                .andExpect(redirectedUrl("/register?registered"))
                .andExpect(authenticated().withUsername(email))
                .andReturn().getRequest().getSession();

        mockMvc.perform(post("/admin/accounts/" + account(email).name().value() + "/suspend").with(csrf())
                .with(user("test-admin").roles("ADMIN")))
                .andExpect(flash().attribute("notice", containsString("ended 1 live session.")));

        mockMvc.perform(get("/").session(registered))
                .andExpect(redirectedUrl("/login?expired"));
    }

    /**
     * Each sign-in from its own address: the suite shares one context, and with it the
     * per-client login ceiling ({@code auth.login.client}, ten a minute), which other test
     * classes spend from the default address.
     */
    private static RequestPostProcessor fromAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private MockHttpSession signIn(String email, String password, String address) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login").with(csrf()).with(fromAddress(address))
                .param("username", email).param("password", password))
                .andExpect(authenticated().withUsername(email))
                .andReturn().getRequest().getSession();
    }
}
