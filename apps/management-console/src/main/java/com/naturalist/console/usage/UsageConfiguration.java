package com.naturalist.console.usage;

import com.naturalist.usage.WarningPercent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Provides the two beans {@code usage-core}'s {@code @DomainService}
 * {@code UsageQueryImpl}/{@code UsageCommandImpl}/{@code IdentificationBudgetImpl}
 * need beyond their repositories: the configured {@link WarningPercent} and a
 * {@link Clock}. Those adapters themselves and the {@code usage-repository-rdms}
 * repositories (which seed the actual budget limits as {@code UsageCounter}
 * rules — see {@code usage-counters.json}) are discovered automatically by
 * {@code DomainServiceScan} (see {@code CatalogConfiguration}) — they are
 * deliberately not declared as {@code @Bean}s here.
 *
 * <p>{@code WarningPercent} wraps the scalar {@code
 * UsageProperties.warningPercent()} rather than exposing it as a bare
 * {@code int} bean: {@code DomainServiceScan}-registered classes get their
 * constructor arguments resolved by Spring's implicit single-constructor
 * autowiring (by type), and that resolution is reliable for a concrete
 * reference type but not for a primitive.
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
    WarningPercent usageWarningPercent(UsageProperties properties) {
        return new WarningPercent(properties.warningPercent());
    }

    @Bean
    Clock usageClock() {
        return Clock.systemUTC();
    }
}
