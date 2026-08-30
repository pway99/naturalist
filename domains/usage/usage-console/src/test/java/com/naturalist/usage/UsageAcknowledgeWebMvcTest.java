package com.naturalist.usage;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acknowledging a real alert removes it from the active list, and the
 * {@code /admin/usage.json} payload serializes the alert's runtime value types
 * ({@code counter()}, {@code id()}) as bare values via the kernel's
 * {@code @JsonValue} unwrapping — not the id/name-validity envelope. A DB-free-of-scan,
 * security-free {@code @WebMvcTest} slice ({@link UsageControllerTestConfig}): a single
 * {@code reserve} under {@code warning-percent=1} crosses the GLOBAL DAILY warning
 * threshold, producing exactly one alert. The console-wide usage-alert banner on the
 * home page (interceptor + admin gating) is asserted app-side in
 * {@code console.auth.UsageAlertBannerWebMvcTest}, not here.
 */
@WebMvcTest
class UsageAcknowledgeWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = UsageControllerTestConfig.DATABASE;

    private static final NaturalistName TEST_NATURALIST = NaturalistName.of("usage-ack-webmvc-test");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsageQuery monitor;

    @Autowired
    IdentificationBudget budget;

    @Test
    void acknowledge_removesAlertFromActiveList_andJsonSerializesRuntimeTypesAsBareValues() throws Exception {
        budget.reserve(TEST_NATURALIST);

        List<UsageAlert> alertsBefore = monitor.activeAlerts();
        assertThat(alertsBefore).hasSize(1);
        UsageAlert alert = alertsBefore.get(0);
        String id = alert.id().value().toString();

        mockMvc.perform(get("/admin/usage.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.alerts[0].counter").value("identification"))
                .andExpect(jsonPath("$.alerts[0].id").value(id));

        mockMvc.perform(post("/admin/usage/alerts/{id}/ack", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usage"));

        assertThat(monitor.activeAlerts())
                .extracting(a -> a.id().value())
                .doesNotContain(alert.id().value());
    }
}
