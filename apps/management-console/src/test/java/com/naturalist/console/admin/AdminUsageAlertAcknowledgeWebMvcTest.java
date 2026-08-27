package com.naturalist.console.admin;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.usage.IdentificationBudget;
import com.naturalist.usage.UsageAlert;
import com.naturalist.usage.UsageQuery;
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
 * gets its own isolated Spring context via {@code @TestPropertySource}: the
 * event-log redesign moved the actual budget limits out of application
 * properties and into the seeded {@code UsageCounter} rules in {@code
 * usage-counters.json} (fixed at {@code PER_USER}/daily=10, {@code
 * GLOBAL}/daily=50, {@code GLOBAL}/monthly=650 — see that file), so a limit
 * can no longer be shrunk per test via a property override. Instead this
 * class overrides only {@code warning-percent}, the one budget knob still
 * property-driven: {@code warning-percent=1} makes the very first
 * {@link IdentificationBudget#reserve} cross the GLOBAL DAILY WARNING
 * threshold ({@code ceil(50*1/100)=1}) deterministically — the monthly rule
 * (limit 650) cannot be made to cross on a single call with an integer
 * percent, since the smallest achievable non-zero threshold there is {@code
 * ceil(650*1/100)=7}. A different property set still means Spring caches
 * this class an entirely separate {@code ApplicationContext} (and so a
 * fresh in-memory {@code NaturalistDatabase}), so this class's counters
 * stay isolated from every other test class exactly as before.
 *
 * <p>The {@code /admin/usage.json} assertions also fold in the C2
 * runtime-serialization check: {@link UsageAlert#counter()} and
 * {@link UsageAlert#id()} must serialize as bare strings ({@code
 * "identification"}, a bare UUID) via the kernel's {@code @JsonValue}
 * unwrapping, not as the id/name-validity envelope Jackson would otherwise
 * emit.
 *
 * <p>The banner-gating assertions (fix round 1) reuse this same single
 * alert rather than reserving a second time in their own test method: the
 * WARNING dedup key only fires once per period, so a second {@code
 * reserve()} call anywhere in this class (Spring caches one {@code
 * ApplicationContext}, and so one counter/alert store, per test class)
 * would not produce a second alert to check — see the checked-in history of
 * this file for the failure that taught us that.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "naturalist.usage.warning-percent=1"
})
class AdminUsageAlertAcknowledgeWebMvcTest {

    private static final NaturalistName TEST_NATURALIST = NaturalistName.of("admin-usage-ack-webmvc-test");

    @Autowired
    WebApplicationContext context;

    @Autowired
    IdentificationBudget budget;

    @Autowired
    UsageQuery monitor;

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

        // Fix round 1: the console-wide banner (NaturalistHeaderInterceptor +
        // CurrentNaturalistView.isAdmin()) must render for ADMIN while this
        // alert is pending, and must NOT render for a non-ADMIN authenticated
        // visitor or an anonymous one — checked here, before the ack below
        // removes the only alert this isolated context has.
        String adminHome = mockMvc.perform(get("/").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(adminHome).contains("usage-alert-banner");

        String naturalistHome = mockMvc.perform(get("/").with(user("alice").roles("NATURALIST")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(naturalistHome).doesNotContain("usage-alert-banner");

        String anonymousHome = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(anonymousHome).doesNotContain("usage-alert-banner");

        mockMvc.perform(post("/admin/usage/alerts/{id}/ack", id)
                        .with(user("naturalist").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usage"));

        assertThat(monitor.activeAlerts())
                .extracting(a -> a.id().value())
                .doesNotContain(alert.id().value());

        // The banner should clear once the only pending alert is acknowledged.
        String adminHomeAfterAck = mockMvc.perform(get("/").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(adminHomeAfterAck).doesNotContain("usage-alert-banner");
    }
}
