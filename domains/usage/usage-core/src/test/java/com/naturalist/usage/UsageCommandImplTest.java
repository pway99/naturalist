package com.naturalist.usage;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Relocated from {@code UsageBudgetServiceTest} when the combined
 * {@code UsageBudgetService} was split into {@link UsageQueryImpl} (reads, gated
 * by the N+1 select gate) and {@link UsageCommandImpl} (writes), then rewritten
 * again when tally upserts gave way to an append-only {@link UsageEvent} log
 * counted per {@link UsageCounter} rule. Every scenario here exercises
 * {@link UsageCommandImpl#reserve}; the ones that also assert on usage state read
 * it back through {@link UsageQueryImpl}. The seeded catalog ({@code
 * usage/usage-counters.json}) carries three active rules for the {@code
 * identification} counter — PER_USER/CALENDAR_DAY limit 10, GLOBAL/CALENDAR_DAY
 * limit 50, GLOBAL/SINCE limit 650 — scenarios that need a tighter limit adjust
 * the relevant seeded rule via {@link #withLimit} before reserving.
 */
class UsageCommandImplTest {

    private static final UsageScope PER_USER = UsageScope.PER_USER;
    private static final UsageScope GLOBAL = UsageScope.GLOBAL;

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC);

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    private static void withLimit(UsageCoreTestContext context, UsageScope scope, WindowKind windowKind, int limit) {
        UsageCounter rule = context.counters().findByCounterName(UsageCounterName.of("identification")).stream()
                .filter(c -> c.scope() == scope && c.windowKind() == windowKind)
                .findFirst()
                .orElseThrow();
        context.counters().save(rule.withLimit(limit));
    }

    @Test
    void per_user_daily_quota_blocks_after_limit() {
        UsageCoreTestContext context = UsageCoreTestContext.create(nte, 80, clock);
        withLimit(context, PER_USER, WindowKind.CALENDAR_DAY, 2);
        UsageCommand command = context.command();
        NaturalistName pat = NaturalistName.of("pat");

        command.reserve(pat);
        command.reserve(pat);

        assertThatThrownBy(() -> command.reserve(pat))
                .isInstanceOf(BudgetExceededException.class)
                .extracting(ex -> ((BudgetExceededException) ex).limitKind())
                .isEqualTo(LimitKind.PER_USER);

        assertThat(context.events().findByCounterSince(
                UsageCounterName.of("identification"), null, Instant.EPOCH)).hasSize(2);
    }

    @Test
    void global_daily_cap_blocks_across_users() {
        UsageCoreTestContext context = UsageCoreTestContext.create(nte, 80, clock);
        withLimit(context, GLOBAL, WindowKind.CALENDAR_DAY, 2);
        UsageCommand command = context.command();

        command.reserve(NaturalistName.of("naturalist-a"));
        command.reserve(NaturalistName.of("naturalist-b"));

        assertThatThrownBy(() -> command.reserve(NaturalistName.of("naturalist-c")))
                .isInstanceOf(BudgetExceededException.class)
                .extracting(ex -> ((BudgetExceededException) ex).limitKind())
                .isEqualTo(LimitKind.DAILY);
    }

    @Test
    void rejected_reserve_inserts_no_event() {
        UsageCoreTestContext context = UsageCoreTestContext.create(nte, 80, clock);
        withLimit(context, PER_USER, WindowKind.CALENDAR_DAY, 1);
        UsageCommand command = context.command();
        NaturalistName pat = NaturalistName.of("pat");

        command.reserve(pat);
        assertThatThrownBy(() -> command.reserve(pat)).isInstanceOf(BudgetExceededException.class);

        assertThat(context.query().snapshot().dailyUsed()).isEqualTo(1);
    }

    @Test
    void warning_and_hard_stop_alerts_recorded_once() {
        UsageCoreTestContext context = UsageCoreTestContext.create(nte, 80, clock);
        withLimit(context, GLOBAL, WindowKind.SINCE, 5);
        UsageCommand command = context.command();

        for (int i = 0; i < 5; i++) {
            command.reserve(NaturalistName.of("monthly-user-" + i));
        }
        for (int i = 0; i < 3; i++) {
            NaturalistName extra = NaturalistName.of("monthly-extra-" + i);
            assertThatThrownBy(() -> command.reserve(extra))
                    .isInstanceOf(BudgetExceededException.class);
        }

        List<UsageAlert> alerts = context.alerts().getUnacknowledged();
        assertThat(alerts).filteredOn(a -> a.kind() == AlertKind.WARNING).hasSize(1);
        assertThat(alerts).filteredOn(a -> a.kind() == AlertKind.HARD_STOP).hasSize(1);
    }

    @Test
    void entitled_naturalist_bypasses_public_rules() {
        UsageCoreTestContext context = UsageCoreTestContext.create(
                nte, naturalist -> true, 80, clock);
        withLimit(context, GLOBAL, WindowKind.CALENDAR_DAY, 0);
        UsageCommand command = context.command();
        NaturalistName entitled = NaturalistName.of("entitled-naturalist");

        command.reserve(entitled);

        assertThat(context.events().findByCounterSince(
                UsageCounterName.of("identification"), entitled, Instant.EPOCH)).hasSize(1);
    }

    @Test
    void concurrent_reserves_do_not_overshoot() throws InterruptedException {
        UsageCoreTestContext context = UsageCoreTestContext.create(nte, 80, clock);
        withLimit(context, GLOBAL, WindowKind.CALENDAR_DAY, 100);
        UsageCommand command = context.command();

        int threadCount = 16;
        int totalAttempts = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        AtomicInteger succeeded = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(totalAttempts);

        for (int i = 0; i < totalAttempts; i++) {
            NaturalistName naturalist = NaturalistName.of("concurrent-user-" + i);
            pool.submit(() -> {
                try {
                    command.reserve(naturalist);
                    succeeded.incrementAndGet();
                } catch (BudgetExceededException expected) {
                    // expected once the global daily cap is reached
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(succeeded.get()).isEqualTo(100);
    }
}
