package com.naturalist.console.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The app security config protects domain routes, using insects pages as a
 * representative example: an anonymous request is bounced to login, and a
 * state-changing POST without a CSRF token is forbidden. App-level (full
 * {@code @SpringBootTest} with the real security filter chain) because it asserts
 * {@code SecurityConfiguration} behavior, not {@code InsectsController} rendering —
 * which lives in {@code insects-console}'s {@code InsectsCatalogWebMvcTest} slice.
 */
@SpringBootTest
class InsectsRouteSecurityWebMvcTest {

    /** Minimal valid JPEG: FF D8 FF E0 header + padding. */
    private static final byte[] TINY_JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void catalogListing_anonymous_isPermitted() throws Exception {
        // Anonymous browsing of the public catalog (the inversion): a rendered
        // rank-listing page returns 200 rather than being bounced to login.
        // (GET /insects itself only redirects to /insects/orders, so it cannot
        // distinguish "permitted" from "login bounce" — this asserts the target.)
        mockMvc.perform(get("/insects/orders"))
                .andExpect(status().isOk());
    }

    @Test
    void identify_anonymous_redirectsToLogin() throws Exception {
        var file = new MockMultipartFile("image", "test.jpg", "image/jpeg", TINY_JPEG);
        mockMvc.perform(multipart("/insects/identify").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login"));
    }

    @Test
    void identify_authenticatedWithoutVision_isForbidden() throws Exception {
        var file = new MockMultipartFile("image", "test.jpg", "image/jpeg", TINY_JPEG);
        mockMvc.perform(multipart("/insects/identify").file(file).with(csrf())
                        .with(user("naturalist")
                                .authorities(new SimpleGrantedAuthority("ROLE_NATURALIST"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void identify_authenticatedWithVision_passesSecurityGate() throws Exception {
        var file = new MockMultipartFile("image", "test.jpg", "image/jpeg", TINY_JPEG);
        mockMvc.perform(multipart("/insects/identify").file(file).with(csrf())
                        .with(user("naturalist").authorities(
                                new SimpleGrantedAuthority("ROLE_NATURALIST"),
                                new SimpleGrantedAuthority("VISION"))))
                // VISION clears the authorization gate; the request reaches the controller,
                // which redirects because a plain test user is not a NaturalistPrincipal.
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void addImage_anonymous_redirectsToLogin() throws Exception {
        var file = new MockMultipartFile("image", "test.jpg", "image/jpeg", TINY_JPEG);
        mockMvc.perform(multipart("/insects/battus-philenor/images").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login"));
    }

    @Test
    void addImage_missingCsrf_isForbidden() throws Exception {
        var file = new MockMultipartFile("image", "test.jpg", "image/jpeg", TINY_JPEG);
        mockMvc.perform(multipart("/insects/battus-philenor/images").file(file)
                        .with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
