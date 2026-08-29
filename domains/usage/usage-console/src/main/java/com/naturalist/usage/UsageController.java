package com.naturalist.usage;

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
 * Renders {@code /admin/usage} — the identification-budget monitoring surface. A
 * thin read of the usage ports ({@link UsageQuery} for display, {@link UsageCommand}
 * for acknowledge) plus the configured {@link WarningPercent}, with every display
 * decision (gauge colour) computed here so the template stays logic-free.
 *
 * <p>Lives in {@code usage-console} and depends only on {@code usage-api}: it takes
 * the warning threshold as the injected {@link WarningPercent} value object (the app's
 * {@code UsageConfiguration} supplies that bean) rather than the app-level
 * {@code UsageProperties}, so the console never reaches into the composition root. The
 * {@code /admin/usage} route is secured app-side; this controller carries no security.
 */
@Controller
public class UsageController {

    private final UsageQuery usageQuery;
    private final UsageCommand usageCommand;
    private final WarningPercent warningPercent;

    UsageController(UsageQuery usageQuery, UsageCommand usageCommand, WarningPercent warningPercent) {
        this.usageQuery = usageQuery;
        this.usageCommand = usageCommand;
        this.warningPercent = warningPercent;
    }

    @GetMapping("/admin/usage")
    String usage(Model model) {
        UsageSnapshot snapshot = usageQuery.snapshot();
        int warning = warningPercent.value();

        model.addAttribute("snapshot", snapshot);
        model.addAttribute("alerts", usageQuery.activeAlerts());
        model.addAttribute("gauges", List.of(
                gauge("Daily", snapshot.dailyUsed(), snapshot.dailyLimit(), warning),
                gauge("Monthly", snapshot.monthlyUsed(), snapshot.monthlyLimit(), warning)
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
