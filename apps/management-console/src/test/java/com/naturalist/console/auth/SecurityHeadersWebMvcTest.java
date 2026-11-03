package com.naturalist.console.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The response headers {@code SecurityConfiguration} pins (ADR-028): a Content-Security-Policy
 * that keeps page scripts from reaching any host but this one, forms from posting elsewhere
 * and the app from being framed -- and nothing stricter, since the templates rely on inline
 * styles and scripts -- plus {@code nosniff} and the {@code no-referrer} policy.
 */
@SpringBootTest
class SecurityHeadersWebMvcTest {

    private static final String POLICY = "connect-src 'self'; form-action 'self'; frame-ancestors 'none'";

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void everyPage_carriesTheConnectFormAndFrameDirectives_andNothingStricter() throws Exception {
        for (var path : List.of("/", "/login", "/insects/orders")) {
            var csp = mockMvc.perform(get(path)).andReturn().getResponse().getHeader("Content-Security-Policy");
            assertThat(csp).as(path).isEqualTo(POLICY);
            assertThat(csp).doesNotContain("default-src", "script-src", "style-src", "frame-src");
        }
    }

    @Test
    void identifyPage_carriesThePolicy_andNosniff() throws Exception {
        var response = mockMvc.perform(get("/insects/identify").with(user("naturalist").authorities(
                        new SimpleGrantedAuthority("ROLE_NATURALIST"), new SimpleGrantedAuthority("VISION"))))
                .andReturn().getResponse();
        assertThat(response.getHeader("Content-Security-Policy")).isEqualTo(POLICY);
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
    }
}
