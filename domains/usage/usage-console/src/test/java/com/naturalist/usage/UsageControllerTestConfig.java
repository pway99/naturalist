package com.naturalist.usage;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.spring.console.ConsoleSliceRenderConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Spring Boot test composition for {@link UsageController} slice tests
 * ({@code @WebMvcTest}). Wires the real, repository-backed usage trio
 * ({@link UsageQuery} / {@link UsageCommand} / {@link IdentificationBudget}) from
 * {@link UsagesTestContext} over one shared database, so a slice can reserve budget
 * through {@code IdentificationBudget} and read the result back through
 * {@code UsageQuery} — a Mockito stub of one would desync it from the others.
 * Full-page rendering comes from the shared {@link ConsoleSliceRenderConfiguration}.
 * No {@code DomainServiceScan}, no Spring Security.
 */
@SpringBootConfiguration
@Import(ConsoleSliceRenderConfiguration.class)
class UsageControllerTestConfig {

    /**
     * warning-percent=1 so a single {@code reserve} crosses the GLOBAL DAILY warning
     * threshold ({@code ceil(50*1/100)=1}) deterministically, letting the acknowledge
     * slice create a real alert. It does not affect the limit/per-user-count assertions.
     */
    static final int WARNING_PERCENT = 1;

    /** Fixed clock keeps the window-based snapshot deterministic across day/month boundaries. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);

    /** Shared per-test-reset database; the slice tests {@code @RegisterExtension} this instance. */
    static final NaturalistTestExtension DATABASE = NaturalistTestExtension.create();

    @Bean
    UsagesTestContext usagesTestContext() {
        return UsagesTestContext.create(DATABASE, WARNING_PERCENT, CLOCK);
    }

    @Bean
    UsageController usageController(UsagesTestContext context) {
        return new UsageController(
                context.usageQuery(), context.usageCommand(), new WarningPercent(WARNING_PERCENT));
    }

    @Bean
    UsageQuery usageQuery(UsagesTestContext context) {
        return context.usageQuery();
    }

    @Bean
    IdentificationBudget identificationBudget(UsagesTestContext context) {
        return context.identificationBudget();
    }
}
