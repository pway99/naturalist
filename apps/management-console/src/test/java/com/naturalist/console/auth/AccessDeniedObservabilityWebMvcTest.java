package com.naturalist.console.auth;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every 403 the security chain gives a signed-in caller is counted by {@link ObservedAccessDeniedHandler}
 * under the rule that refused it, and the response is still a 403. A private {@link SimpleMeterRegistry}
 * on the global composite isolates each test's counts.
 */
@SpringBootTest
class AccessDeniedObservabilityWebMvcTest {

    private static final byte[] TINY_JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };
    private static final SimpleGrantedAuthority NATURALIST = new SimpleGrantedAuthority("ROLE_NATURALIST");

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Metrics.addRegistry(meters);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void unbind() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void memberWrite_unverified_isCountedAsEmailVerified() throws Exception {
        mockMvc.perform(post("/insects/battus-philenor/observe").with(csrf())
                .with(user("naturalist").authorities(NATURALIST)))
                .andExpect(status().isForbidden());

        assertThat(violations("emailVerified")).isEqualTo(1);
        assertThat(violations("hasVision")).isZero();
    }

    @Test
    void identify_withoutVision_isCountedAsHasVision() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", TINY_JPEG);
        mockMvc.perform(multipart("/insects/identify").file(file).with(csrf())
                .with(user("naturalist").authorities(NATURALIST)))
                .andExpect(status().isForbidden());

        assertThat(violations("hasVision")).isEqualTo(1);
    }

    @Test
    void missingCsrfToken_isCountedAsCsrfTokenValid() throws Exception {
        mockMvc.perform(post("/insects/battus-philenor/observe")
                .with(user("naturalist").authorities(NATURALIST)))
                .andExpect(status().isForbidden());

        assertThat(violations("csrfTokenValid")).isEqualTo(1);
        assertThat(violations("emailVerified")).isZero();
    }

    @Test
    void adminPage_withoutAdminRole_isCountedAsHasAdminRole() throws Exception {
        mockMvc.perform(get("/admin/usage").with(user("naturalist").authorities(NATURALIST)))
                .andExpect(status().isForbidden());

        assertThat(violations("hasAdminRole")).isEqualTo(1);
    }

    private double violations(String rule) {
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                .tag("name", "ObservedAccessDeniedHandler.handle." + rule).counter();
        return counter == null ? 0 : counter.count();
    }
}
