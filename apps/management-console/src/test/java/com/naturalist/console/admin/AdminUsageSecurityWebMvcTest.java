package com.naturalist.console.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression coverage for the {@code /admin/usage} branch of the security
 * chain (task C3 of identification cost controls) — mirrors
 * {@link AdminSecurityWebMvcTest} but targets the concrete mapped path
 * rather than an unmapped {@code /admin/anything}, so a future loosening of
 * the {@code /admin/**} matcher that still happens to let this specific
 * controller through would be caught here even if it slipped past the
 * generic test.
 */
@SpringBootTest
class AdminUsageSecurityWebMvcTest {

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
    void anonymous_usage_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/usage"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login"));
    }

    @Test
    @WithMockUser(roles = "NATURALIST")
    void nonAdminAuthenticated_usage_isForbidden() throws Exception {
        mockMvc.perform(get("/admin/usage"))
                .andExpect(status().isForbidden());
    }
}
