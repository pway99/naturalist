package com.naturalist.console.admin;

import com.naturalist.console.usage.UsageProperties;
import com.naturalist.usage.UsageAlert;
import com.naturalist.usage.UsageAlertId;
import com.naturalist.usage.UsageCommand;
import com.naturalist.usage.UsagePolicy;
import com.naturalist.usage.UsageQuery;
import com.naturalist.usage.UsageSnapshot;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Renders {@code /admin/usage} — the identification-budget monitoring
 * surface for task C1 of the identification cost controls effort. Mirrors
 * {@link AdminResilienceController}: a thin read of the kernel-facing ports
 * ({@link UsageQuery} for display, {@link UsageCommand} for acknowledge) plus
 * the configured warning threshold, with every display decision (gauge colour)
 * computed here so the template stays logic-free.
 */
@Controller
public class AdminUsageController {

    private final UsageQuery usageQuery;
    private final UsageCommand usageCommand;
    private final UsageProperties usageProperties;

    AdminUsageController(UsageQuery usageQuery, UsageCommand usageCommand, UsageProperties usageProperties) {
        this.usageQuery = usageQuery;
        this.usageCommand = usageCommand;
        this.usageProperties = usageProperties;
    }

    @GetMapping("/admin/usage")
    String usage(Model model) {
        UsageSnapshot snapshot = usageQuery.snapshot();
        int warningPercent = usageProperties.warningPercent();

        model.addAttribute("snapshot", snapshot);
        model.addAttribute("alerts", usageQuery.activeAlerts());
        model.addAttribute("gauges", List.of(
                gauge("Daily", snapshot.dailyUsed(), snapshot.dailyLimit(), warningPercent),
                gauge("Monthly", snapshot.monthlyUsed(), snapshot.monthlyLimit(), warningPercent)
        ));
        return "admin/usage";
    }

    @GetMapping(value = "/admin/usage.json", produces = "application/json")
    @ResponseBody
    UsageReport report() {
        return new UsageReport(usageQuery.snapshot(), usageQuery.activeAlerts());
    }

    @PostMapping("/admin/usage/alerts/{id}/ack")
    String acknowledge(@PathVariable String id) {
        UUID value;
        try {
            value = UUID.fromString(id);
            usageCommand.acknowledge(UsageAlertId.of(value));
        } catch (IllegalArgumentException | NoSuchElementException notAcknowledgeable) {
            // Either the path segment isn't a UUID at all, or it is a
            // syntactically valid UUID that doesn't name an existing alert
            // (UsageCommandImpl#acknowledge does getByName(id).orElseThrow()).
            // Neither is an alert to acknowledge — bounce back to the
            // dashboard instead of a 500.
            return "redirect:/admin/usage";
        }
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
