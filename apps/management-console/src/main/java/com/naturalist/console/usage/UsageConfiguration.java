package com.naturalist.console.usage;

import com.naturalist.usage.UsageLimits;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Provides the two beans {@code usage-core}'s {@code @DomainService}
 * {@code UsageBudgetService} needs beyond its repositories: the configured
 * {@link UsageLimits} and a {@link Clock}. The service itself and the three
 * {@code usage-repository-rdms} repositories are discovered automatically
 * by {@code DomainServiceScan} (see {@code CatalogConfiguration}) — they are
 * deliberately not declared as {@code @Bean}s here.
 *
 * <p>{@link EnableScheduling} is the app's first use of Spring scheduling —
 * added here rather than on the application class because the only
 * scheduled job so far ({@link AlertDispatchJob}) is usage-domain-specific.
 * It registers the default {@code TaskScheduler} that drives
 * {@code @Scheduled} methods; it does not itself run anything.
 */
@Configuration
@EnableConfigurationProperties(UsageProperties.class)
@EnableScheduling
class UsageConfiguration {

    @Bean
    UsageLimits usageLimits(UsageProperties properties) {
        return new UsageLimits(
                properties.perUserDaily(),
                properties.globalRatePerMinute(),
                properties.globalDaily(),
                properties.globalMonthly(),
                properties.warningPercent());
    }

    @Bean
    Clock usageClock() {
        return Clock.systemUTC();
    }
}
