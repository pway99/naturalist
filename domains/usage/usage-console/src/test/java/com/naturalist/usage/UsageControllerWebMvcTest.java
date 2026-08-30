package com.naturalist.usage;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Render + JSON + acknowledge-error behavior for {@link UsageController}. A
 * DB-free-of-scan, security-free {@code @WebMvcTest} slice
 * ({@link UsageControllerTestConfig}) over the real repository-backed usage trio,
 * so {@code showsMonthlyNumberAndPerUserRow} drives a genuine
 * {@link IdentificationBudget#reserve} and reads the count back off the page.
 * The {@code /admin/usage} route's security (login redirect, admin-only) is
 * asserted app-side in {@code console.admin.AdminUsageSecurityWebMvcTest}.
 */
@WebMvcTest
class UsageControllerWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = UsageControllerTestConfig.DATABASE;

    private static final NaturalistName TEST_NATURALIST = NaturalistName.of("usage-webmvc-test");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsageQuery monitor;

    @Autowired
    IdentificationBudget budget;

    @Test
    void usage_rendersOk() throws Exception {
        mockMvc.perform(get("/admin/usage"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void usage_showsMonthlyNumberAndPerUserRow() throws Exception {
        var limits = monitor.snapshot();

        budget.reserve(TEST_NATURALIST);
        budget.reserve(TEST_NATURALIST);

        var html = mockMvc.perform(get("/admin/usage"))
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
    void usageJson_returnsSnapshotAndAlerts() throws Exception {
        var limits = monitor.snapshot();

        mockMvc.perform(get("/admin/usage.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.usage.monthlyLimit").value(limits.monthlyLimit()))
                .andExpect(jsonPath("$.alerts").isArray());
    }

    @Test
    void acknowledge_malformedId_redirectsInsteadOf500() throws Exception {
        mockMvc.perform(post("/admin/usage/alerts/{id}/ack", "not-a-uuid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usage"));
    }

    @Test
    void acknowledge_unknownId_redirectsInsteadOf500() throws Exception {
        String unknownButValidUuid = UsageAlertId.create().value().toString();

        mockMvc.perform(post("/admin/usage/alerts/{id}/ack", unknownButValidUuid))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usage"));
    }
}
