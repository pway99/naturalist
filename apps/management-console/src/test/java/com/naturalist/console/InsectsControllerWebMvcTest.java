package com.naturalist.console;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end smoke test for the {@code /insects} catalog view.
 *
 * <p>Exercises the full Spring slice — controller wiring, the JTE
 * {@link gg.jte.TemplateEngine} bean resolved from {@link JteConfiguration},
 * and the security filter chain. Complements the unit-level tests:
 * <ul>
 *   <li>{@code InsectsControllerTest} (insects-console) — controller model contract.</li>
 *   <li>{@code InsectsListTemplateTest} (insects-console) — template render against a fixture.</li>
 *   <li>This test — confirms the two pieces meet end-to-end under Spring + security.</li>
 * </ul>
 */
@SpringBootTest
class InsectsControllerWebMvcTest {

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
    void insectsListing_respondsOk_andRendersCatalogHeading() throws Exception {
        mockMvc.perform(get("/insects").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Insect Species")))
                .andExpect(content().string(containsString("species documented at Oak Vista")));
    }

    @Test
    void insectsListing_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/insects"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login"));
    }

    @Test
    void detail_authenticated_rendersAddPhotoForm() throws Exception {
        mockMvc.perform(get("/insects/tachinid-fly").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add Photo")))
                .andExpect(content().string(containsString("name=\"resourceName\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void addImage_authenticatedWithCsrf_redirectsToDetail() throws Exception {
        mockMvc.perform(post("/insects/tachinid-fly/images")
                        .with(user("naturalist").roles("ADMIN"))
                        .with(csrf())
                        .param("resourceName", "IMG_TEST_NEW.HEIC"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/tachinid-fly"));
    }

    @Test
    void addImage_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/insects/tachinid-fly/images")
                        .with(csrf())
                        .param("resourceName", "IMG_ANON.HEIC"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login"));
    }

    @Test
    void addImage_missingCsrf_isForbidden() throws Exception {
        mockMvc.perform(post("/insects/tachinid-fly/images")
                        .with(user("naturalist").roles("ADMIN"))
                        .param("resourceName", "IMG_NO_CSRF.HEIC"))
                .andExpect(status().isForbidden());
    }
}
