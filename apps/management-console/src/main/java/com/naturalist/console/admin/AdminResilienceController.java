package com.naturalist.console.admin;

import com.naturalist.resilience.Resilience;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Renders {@code /admin/resilience} — the registered-strategy diagnostic
 * surface described in {@code docs/plans/admin-console.md}.
 *
 * <p>Depends on the kernel {@link Resilience} facade only. Resilience4j
 * (or any future vendor) is not imported here — vendor-specific state
 * (open/closed/half-open, metrics) is a follow-up that crosses the
 * facade boundary and justifies an adapter-side extension rather than
 * a vendor import in the controller.
 */
@Controller
public class AdminResilienceController {

    private final Resilience resilience;

    AdminResilienceController(Resilience resilience) {
        this.resilience = resilience;
    }

    @GetMapping("/admin/resilience")
    String resilience(Model model) {
        model.addAttribute("groups", List.of(
                new Group("Retry", sorted(resilience.retryNames())),
                new Group("Timeout", sorted(resilience.timeoutNames())),
                new Group("Circuit Breaker", sorted(resilience.circuitBreakerNames())),
                new Group("Bulkhead", sorted(resilience.bulkheadNames())),
                new Group("Rate Limiter", sorted(resilience.rateLimiterNames()))
        ));
        return "admin/resilience";
    }

    private static List<String> sorted(Set<String> names) {
        return List.copyOf(new TreeSet<>(names));
    }

    public record Group(String primitive, List<String> names) {
    }
}
