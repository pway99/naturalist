package com.naturalist.console.admin;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.usage.IdentificationBudget;
import com.naturalist.usage.UsageMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end render + auth assertions for {@code /admin/usage} (admin
 * console cost-controls C1). Mirrors
 * {@link AdminResilienceControllerWebMvcTest}, exercising the real,
 * repository-backed {@link UsageMonitor} bean rather than a stub: the
 * single {@code UsageBudgetService} implements both {@link
 * IdentificationBudget} and {@code UsageMonitor}, so a Mockito bean
 * override of one interface replaces the shared bean the insects
 * console also depends on for the other, which fails app-context
 * startup. Instead, the "known monthly number" assertion drives the
 * real {@link IdentificationBudget#reserve} against a naturalist name
 * unique to this test, then reads it back off the rendered page — the
 * monthly/daily/rate totals stay untouched by any other WebMvc test in
 * this module (none currently call {@code reserve}), and the increment
 * used here (2) stays far under every configured limit.
 */
@SpringBootTest
class AdminUsageControllerWebMvcTest {

    private static final NaturalistName TEST_NATURALIST = NaturalistName.of("admin-usage-webmvc-test");

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
    void anonymous_usage_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/usage"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void admin_usage_rendersOk() throws Exception {
        mockMvc.perform(get("/admin/usage").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void admin_usage_showsMonthlyNumberAndPerUserRow() throws Exception {
        var limits = monitor.snapshot();

        budget.reserve(TEST_NATURALIST);
        budget.reserve(TEST_NATURALIST);

        var html = mockMvc.perform(get("/admin/usage").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html)
                .as("monthly limit is a stable, config-driven number")
                .contains(String.valueOf(limits.monthlyLimit()));
        assertThat(html)
                .as("the per-user row for the naturalist this test reserved against")
                .contains(TEST_NATURALIST.value())
                .contains(">2<");
    }

    @Test
    void admin_usageJson_returnsSnapshotAndAlerts() throws Exception {
        var limits = monitor.snapshot();

        mockMvc.perform(get("/admin/usage.json").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.usage.monthlyLimit").value(limits.monthlyLimit()))
                .andExpect(jsonPath("$.alerts").isArray());
    }
}
