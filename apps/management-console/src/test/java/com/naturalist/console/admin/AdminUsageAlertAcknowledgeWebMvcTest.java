package com.naturalist.console.admin;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.usage.IdentificationBudget;
import com.naturalist.usage.UsageAlert;
import com.naturalist.usage.UsageMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the {@code /admin/usage/alerts/{id}/ack} endpoint (task C3)
 * against a REAL alert, which requires crossing a threshold. Rather than
 * hammering the module's shared, real-configured {@link IdentificationBudget}
 * (see the javadoc on {@link AdminUsageControllerWebMvcTest}), this class
 * gets its own isolated Spring context via {@code @TestPropertySource}: a
 * tiny {@code global-monthly=2} / {@code warning-percent=50} limit makes the
 * very first {@link IdentificationBudget#reserve} cross the monthly WARNING
 * threshold ({@code ceil(2*50/100)=1}) deterministically, without disturbing
 * any other test class's counters — a different property set means Spring
 * caches this class an entirely separate {@code ApplicationContext} (and so
 * a fresh in-memory {@code NaturalistDatabase}).
 *
 * <p>The {@code /admin/usage.json} assertions also fold in the C2
 * runtime-serialization check: {@link UsageAlert#counter()} and
 * {@link UsageAlert#id()} must serialize as bare strings ({@code
 * "identification"}, a bare UUID) via the kernel's {@code @JsonValue}
 * unwrapping, not as the id/name-validity envelope Jackson would otherwise
 * emit.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "naturalist.usage.global-monthly=2",
        "naturalist.usage.warning-percent=50",
        "naturalist.usage.global-daily=99",
        "naturalist.usage.per-user-daily=99",
        "naturalist.usage.global-rate-per-minute=99"
})
class AdminUsageAlertAcknowledgeWebMvcTest {

    private static final NaturalistName TEST_NATURALIST = NaturalistName.of("admin-usage-ack-webmvc-test");

    @Autowired
    WebApplicationContext context;

    @Autowired
    IdentificationBudget budget;

    @Autowired
    UsageMonitor monitor;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void acknowledge_removesAlertFromActiveList_andJsonSerializesRuntimeTypesAsBareValues() throws Exception {
        budget.reserve(TEST_NATURALIST);

        List<UsageAlert> alertsBefore = monitor.activeAlerts();
        assertThat(alertsBefore).hasSize(1);
        UsageAlert alert = alertsBefore.get(0);
        String id = alert.id().value().toString();

        mockMvc.perform(get("/admin/usage.json").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.alerts[0].counter").value("identification"))
                .andExpect(jsonPath("$.alerts[0].id").value(id));

        mockMvc.perform(post("/admin/usage/alerts/{id}/ack", id)
                        .with(user("naturalist").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usage"));

        assertThat(monitor.activeAlerts())
                .extracting(a -> a.id().value())
                .doesNotContain(alert.id().value());
    }
}
