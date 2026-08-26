package com.naturalist.console.admin;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.usage.IdentificationBudget;
import com.naturalist.usage.UsageAlertId;
import com.naturalist.usage.UsageQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
 * End-to-end render + auth assertions for {@code /admin/usage} (admin
 * console cost-controls C1). Mirrors
 * {@link AdminResilienceControllerWebMvcTest}, exercising the real,
 * repository-backed {@link UsageQuery} bean rather than a stub: {@link
 * IdentificationBudget} and {@code UsageQuery} are backed by the same
 * shared repository state (via {@code UsageCommand}), so a Mockito bean
 * override of one would desync it from the other, which fails app-context
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
    UsageQuery monitor;

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

    /**
     * Fix round 1, MINOR finding: a malformed {@code {id}} path segment must
     * not blow up as an unhandled {@link IllegalArgumentException} → 500.
     * {@code AdminUsageController#acknowledge} now catches the
     * {@code UUID.fromString} parse failure and bounces back to the
     * dashboard instead.
     */
    @Test
    void acknowledge_malformedId_redirectsInsteadOf500() throws Exception {
        mockMvc.perform(post("/admin/usage/alerts/{id}/ack", "not-a-uuid")
                        .with(user("naturalist").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usage"));
    }

    /**
     * Fix round 2, MINOR finding: a syntactically valid UUID that does not
     * name an existing alert reaches {@code UsageCommandImpl#acknowledge}'s
     * {@code getByName(id).orElseThrow()} and previously surfaced as an
     * unhandled {@link java.util.NoSuchElementException} → 500. The
     * controller's guard now also catches the absent-alert case and bounces
     * back to the dashboard, same as the malformed-id case above.
     */
    @Test
    void acknowledge_unknownId_redirectsInsteadOf500() throws Exception {
        // A real UUIDv7 (via the kernel generator, not UUID.randomUUID()) so the
        // request exercises the "valid id, no matching alert" branch specifically,
        // rather than tripping any upstream version check.
        String unknownButValidUuid = UsageAlertId.create().value().toString();

        mockMvc.perform(post("/admin/usage/alerts/{id}/ack", unknownButValidUuid)
                        .with(user("naturalist").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usage"));
    }
}
