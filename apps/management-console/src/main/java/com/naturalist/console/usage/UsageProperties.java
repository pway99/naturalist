package com.naturalist.console.usage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Property-bound identification cost controls under {@code naturalist.usage.*}.
 * Bound into a {@link com.naturalist.usage.UsageLimits} by
 * {@link UsageConfiguration}; {@code alertEmail} is read separately by the
 * (not-yet-wired) alert-email sender.
 */
@ConfigurationProperties("naturalist.usage")
public record UsageProperties(int perUserDaily,
                               int globalRatePerMinute,
                               int globalDaily,
                               int globalMonthly,
                               int warningPercent,
                               String alertEmail) {
}
