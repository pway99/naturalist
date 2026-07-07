package com.naturalist.console.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies naturalist form login and route authorization alongside the admin.
 */
@SpringBootTest
class NaturalistLoginWebMvcTest {

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
    void naturalist_validCredentials_authenticates() throws Exception {
        mockMvc.perform(formLogin("/login").user("patrick-way").password("durrell"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/"));
    }

    @Test
    void naturalist_wrongPassword_redirectsToError() throws Exception {
        mockMvc.perform(formLogin("/login").user("patrick-way").password("wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login?error"));
    }

    @Test
    void unknownUsername_redirectsToError() throws Exception {
        mockMvc.perform(formLogin("/login").user("no-such-naturalist").password("x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login?error"));
    }

    @Test
    void naturalist_forbiddenFromAdminRoute() throws Exception {
        mockMvc.perform(get("/admin/anything").with(user("patrick-way").roles("NATURALIST")))
                .andExpect(status().isForbidden());
    }
}
