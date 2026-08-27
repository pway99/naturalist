package com.naturalist.console.usage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Property-bound identification cost-control policy under
 * {@code naturalist.usage.*}. The actual budget limits now live as seeded
 * {@code UsageCounter} rules (see {@code usage-counters.json}), not here —
 * this record carries only the app-level policy knobs: the warning
 * threshold percentage (wrapped as a {@link com.naturalist.usage.WarningPercent}
 * bean by {@link UsageConfiguration}) and {@code alertEmail}, read separately
 * by {@link AlertEmailer}.
 */
@ConfigurationProperties("naturalist.usage")
public record UsageProperties(int warningPercent,
                               String alertEmail) {
}
