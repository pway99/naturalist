package com.naturalist.console.admin;

import com.naturalist.console.usage.UsageProperties;
import com.naturalist.usage.UsageAlert;
import com.naturalist.usage.UsageAlertId;
import com.naturalist.usage.UsageMonitor;
import com.naturalist.usage.UsagePolicy;
import com.naturalist.usage.UsageSnapshot;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.UUID;

/**
 * Renders {@code /admin/usage} — the identification-budget monitoring
 * surface for task C1 of the identification cost controls effort. Mirrors
 * {@link AdminResilienceController}: a thin read of the kernel-facing port
 * ({@link UsageMonitor}) plus the configured warning threshold, with every
 * display decision (gauge colour) computed here so the template stays
 * logic-free.
 */
@Controller
public class AdminUsageController {

    private final UsageMonitor usageMonitor;
    private final UsageProperties usageProperties;

    AdminUsageController(UsageMonitor usageMonitor, UsageProperties usageProperties) {
        this.usageMonitor = usageMonitor;
        this.usageProperties = usageProperties;
    }

    @GetMapping("/admin/usage")
    String usage(Model model) {
        UsageSnapshot snapshot = usageMonitor.snapshot();
        int warningPercent = usageProperties.warningPercent();

        model.addAttribute("snapshot", snapshot);
        model.addAttribute("alerts", usageMonitor.activeAlerts());
        model.addAttribute("gauges", List.of(
                gauge("Daily", snapshot.dailyUsed(), snapshot.dailyLimit(), warningPercent),
                gauge("Monthly", snapshot.monthlyUsed(), snapshot.monthlyLimit(), warningPercent),
                gauge("Rate (per minute)", snapshot.rateUsed(), snapshot.ratePerMinute(), warningPercent)
        ));
        return "admin/usage";
    }

    @GetMapping(value = "/admin/usage.json", produces = "application/json")
    @ResponseBody
    UsageReport report() {
        return new UsageReport(usageMonitor.snapshot(), usageMonitor.activeAlerts());
    }

    @PostMapping("/admin/usage/alerts/{id}/ack")
    String acknowledge(@PathVariable String id) {
        UUID value;
        try {
            value = UUID.fromString(id);
        } catch (IllegalArgumentException malformed) {
            // Malformed path segment (not a UUID at all) — no alert to acknowledge;
            // bounce back to the dashboard instead of a 500.
            return "redirect:/admin/usage";
        }
        usageMonitor.acknowledge(UsageAlertId.of(value));
        return "redirect:/admin/usage";
    }

    private static Gauge gauge(String label, int used, int limit, int warningPercent) {
        String state;
        if (used >= limit) {
            state = "red";
        } else if (used >= UsagePolicy.warningThreshold(limit, warningPercent)) {
            state = "amber";
        } else {
            state = "green";
        }
        return new Gauge(label, used, limit, state);
    }

    public record Gauge(String label, int used, int limit, String state) {
    }

    public record UsageReport(UsageSnapshot usage, List<UsageAlert> alerts) {
    }
}
