package com.naturalist.console.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the {@code /admin/**} branch of the security chain.
 *
 * <p>The path {@code /admin/anything} is deliberately unmapped to keep
 * the assertions about the chain itself, independent of any specific
 * admin controller: anonymous traffic is bounced to {@code /login} by
 * form login; ADMIN-authenticated traffic is allowed to fall through,
 * where the dispatcher returns 404 for the unmapped path; a non-ADMIN
 * authenticated user is forbidden. Per-controller render assertions
 * live alongside their controllers (e.g. {@link AdminResilienceControllerWebMvcTest}).
 */
@SpringBootTest
class AdminSecurityWebMvcTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void anonymous_admin_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/anything"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login"));
    }

    @Test
    void nonAdminAuthenticated_admin_isForbidden() throws Exception {
        mockMvc.perform(get("/admin/anything").with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAuthenticated_admin_passesSecurityChain() throws Exception {
        mockMvc.perform(get("/admin/anything").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}
