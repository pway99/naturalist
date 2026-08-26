package com.naturalist.console.usage;

import com.naturalist.usage.UsageLimits;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides the two beans {@code usage-core}'s {@code @DomainService}
 * {@code UsageBudgetService} needs beyond its repositories: the configured
 * {@link UsageLimits} and a {@link Clock}. The service itself and the three
 * {@code usage-repository-rdms} repositories are discovered automatically
 * by {@code DomainServiceScan} (see {@code CatalogConfiguration}) — they are
 * deliberately not declared as {@code @Bean}s here.
 */
@Configuration
@EnableConfigurationProperties(UsageProperties.class)
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
