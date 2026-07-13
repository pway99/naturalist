package com.naturalist.console.auth;

import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Verifies the shared header renders the logged-in naturalist and a logout control.
 */
@SpringBootTest
class HeaderWebMvcTest {

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
    void home_asNaturalist_showsNameAndLogout() throws Exception {
        var principal = new NaturalistPrincipal(
                NaturalistName.of("patrick-way"), "Patrick", "{bcrypt}x");
        var auth = new UsernamePasswordAuthenticationToken(
                principal, "n/a", principal.getAuthorities());

        mockMvc.perform(get("/").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Patrick")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/logout\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"hidden\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void home_asAdmin_showsAdministratorLabelAndLogout() throws Exception {
        // The admin authenticates as a bare Spring User (not a NaturalistPrincipal),
        // so the header shows the "Administrator" fallback label rather than a naturalist name.
        mockMvc.perform(get("/").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Administrator")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/logout\"")));
    }
}
