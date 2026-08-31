package com.naturalist.console.auth;

import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The shared page header ({@code layout/page.jte}) shows the "My collection" lens
 * toggle only for a signed-in naturalist on an insects page, and hides it off the
 * insects section and for admins. This is an app-level header test, not an
 * {@code InsectsController} test: the form is rendered by the app shell, gated on
 * the {@code insectSection} / naturalist request attributes that
 * {@link NaturalistHeaderInterceptor} publishes, and one case exercises the
 * non-insects home page. It therefore stays in the composition root (full
 * {@code @SpringBootTest} with the real security filter chain) rather than moving
 * to a security-free {@code insects-console} slice.
 */
@SpringBootTest
class HeaderCollectionLensWebMvcTest {

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static UsernamePasswordAuthenticationToken patrick() {
        var p = new NaturalistPrincipal(NaturalistName.of("patrick-way"), "Patrick",
                "gerald.durrell@oakvista.example", "{bcrypt}x", false, true);
        return new UsernamePasswordAuthenticationToken(p, "n/a", p.getAuthorities());
    }

    @Test
    void toggle_showsOnInsectsForNaturalist() throws Exception {
        mockMvc.perform(get("/insects/species").with(authentication(patrick())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/insects/collection-lens\"")))
                .andExpect(content().string(containsString("My collection")));
    }

    @Test
    void toggle_hiddenOffInsects() throws Exception {
        mockMvc.perform(get("/").with(authentication(patrick())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("action=\"/insects/collection-lens\""))));
    }

    @Test
    void toggle_hiddenForAdmin() throws Exception {
        mockMvc.perform(get("/insects/species").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("action=\"/insects/collection-lens\""))));
    }
}
