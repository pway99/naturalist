package com.naturalist.console.auth;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The console-wide usage-alert banner (published by {@link NaturalistHeaderInterceptor}
 * + {@link CurrentNaturalistView#isAdmin()} and rendered by {@code layout/page.jte})
 * shows on the home page for an ADMIN while an alert is pending, and is hidden for a
 * non-admin or anonymous visitor and once the alert is acknowledged. App-level (full
 * {@code @SpringBootTest} with the real security filter chain, interceptor, and home
 * page) — not a {@code UsageController} test; the controller/JSON/acknowledge behavior
 * lives in {@code usage-console}'s {@code UsageAcknowledgeWebMvcTest} slice.
 *
 * <p>{@code warning-percent=1} makes the first {@link IdentificationBudget#reserve}
 * cross the GLOBAL DAILY warning threshold ({@code ceil(50*1/100)=1}), producing exactly
 * one alert; the distinct property set gives this class its own cached context, isolating
 * its counters from other tests.
 *
 * <p>This is the only app-level test that <em>writes</em> — {@code reserve} appends a usage event
 * and raises an alert — which for a while made it the one test able to corrupt the standing
 * {@code naturalist_test} Postgres, since a {@code @SpringBootTest} commits by default and usage
 * events and alerts carry no persistent seed (so those tables must be empty for
 * {@code usage-repository-rdbms}'s ITs, which read whole tables). It writes to the in-memory
 * {@code @MockDomainService} doubles now: this module keeps the rdbms jars off its test
 * classpath entirely, so there is no database to corrupt and {@code activeAlerts()} is
 * deterministic across re-runs.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "naturalist.usage.warning-percent=1"
})
class UsageAlertBannerWebMvcTest {

    private static final NaturalistName TEST_NATURALIST = NaturalistName.of("usage-banner-webmvc-test");

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
    void usageAlertBanner_showsForAdmin_hiddenForOthers_andClearsAfterAck() throws Exception {
        budget.reserve(TEST_NATURALIST);

        List<UsageAlert> alerts = monitor.activeAlerts();
        assertThat(alerts).hasSize(1);
        String id = alerts.get(0).id().value().toString();

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
                .andExpect(status().is3xxRedirection());

        String adminHomeAfterAck = mockMvc.perform(get("/").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(adminHomeAfterAck).doesNotContain("usage-alert-banner");
    }
}
