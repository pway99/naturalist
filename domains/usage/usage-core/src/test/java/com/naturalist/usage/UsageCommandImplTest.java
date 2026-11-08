package com.naturalist.usage;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.test.context.Fake;
import com.naturalist.test.context.Wired;
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
 * the relevant seeded rule via {@link #withLimit} before reserving. The entitled
 * branch is covered by {@link UsageCommandEntitlementTest}, which chooses who is
 * entitled before the command is built.
 */
class UsageCommandImplTest {

    private static final UsageScope PER_USER = UsageScope.PER_USER;
    private static final UsageScope GLOBAL = UsageScope.GLOBAL;

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Fake
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC);

    @Wired
    UsageCommand command;

    @Wired
    UsageQuery query;

    @Wired
    IdentificationBudget identificationBudget;

    @Wired
    UsageRepository.CounterRepository counters;

    @Wired
    UsageRepository.EventRepository events;

    @Wired
    UsageRepository.AlertRepository alerts;

    private void withLimit(UsageScope scope, WindowKind windowKind, int limit) {
        UsageCounter rule = counters.findByCounterName(UsageCounterName.of("identification")).stream()
                .filter(c -> c.scope() == scope && c.windowKind() == windowKind)
                .findFirst()
                .orElseThrow();
        counters.save(rule.withLimit(limit));
    }

    @Test
    void per_user_daily_quota_blocks_after_limit() {
        withLimit(PER_USER, WindowKind.CALENDAR_DAY, 2);
        NaturalistName pat = NaturalistName.of("pat");

        command.reserve(pat);
        command.reserve(pat);

        assertThatThrownBy(() -> command.reserve(pat))
                .isInstanceOf(BudgetExceededException.class)
                .extracting(ex -> ((BudgetExceededException) ex).limitKind())
                .isEqualTo(LimitKind.PER_USER);

        assertThat(events.findByCounterSince(
                UsageCounterName.of("identification"), null, Instant.EPOCH)).hasSize(2);
    }

    @Test
    void global_daily_cap_blocks_across_users() {
        withLimit(GLOBAL, WindowKind.CALENDAR_DAY, 2);

        command.reserve(NaturalistName.of("naturalist-a"));
        command.reserve(NaturalistName.of("naturalist-b"));

        assertThatThrownBy(() -> command.reserve(NaturalistName.of("naturalist-c")))
                .isInstanceOf(BudgetExceededException.class)
                .extracting(ex -> ((BudgetExceededException) ex).limitKind())
                .isEqualTo(LimitKind.DAILY);
    }

    @Test
    void rejected_reserve_inserts_no_event() {
        withLimit(PER_USER, WindowKind.CALENDAR_DAY, 1);
        NaturalistName pat = NaturalistName.of("pat");

        command.reserve(pat);
        assertThatThrownBy(() -> command.reserve(pat)).isInstanceOf(BudgetExceededException.class);

        assertThat(query.snapshot().dailyUsed()).isEqualTo(1);
    }

    @Test
    void warning_and_hard_stop_alerts_recorded_once() {
        withLimit(GLOBAL, WindowKind.SINCE, 5);

        for (int i = 0; i < 5; i++) {
            command.reserve(NaturalistName.of("monthly-user-" + i));
        }
        for (int i = 0; i < 3; i++) {
            NaturalistName extra = NaturalistName.of("monthly-extra-" + i);
            assertThatThrownBy(() -> command.reserve(extra))
                    .isInstanceOf(BudgetExceededException.class);
        }

        List<UsageAlert> unacknowledged = alerts.getUnacknowledged();
        assertThat(unacknowledged).filteredOn(a -> a.kind() == AlertKind.WARNING).hasSize(1);
        assertThat(unacknowledged).filteredOn(a -> a.kind() == AlertKind.HARD_STOP).hasSize(1);
    }

    @Test
    void concurrent_reserves_do_not_overshoot() throws InterruptedException {
        withLimit(GLOBAL, WindowKind.CALENDAR_DAY, 100);

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

    /**
     * A refused identification is one {@code identification-refused} event naming the naturalist
     * and when -- nothing else -- and is counted apart from the {@code identification} budget.
     */
    @Test
    void refused_recordsOneRefusalEventForTheNaturalist_andSpendsNoBudget() {
        NaturalistName pat = NaturalistName.of("pat");

        command.refused(pat);

        assertThat(events.findByCounterSince(UsageCounterName.of("identification-refused"), pat, Instant.EPOCH))
                .singleElement()
                .satisfies(e -> assertThat(e.instant()).isEqualTo(clock.instant()));
        assertThat(events.findByCounterSince(UsageCounterName.of("identification"), null, Instant.EPOCH))
                .isEmpty();
    }

    @Test
    void refused_nullNaturalist_isRejected() {
        assertThatThrownBy(() -> command.refused(null))
                .isInstanceOf(InvariantViolationException.class);
        assertThat(events.findByCounterSince(UsageCounterName.of("identification-refused"), null, Instant.EPOCH))
                .isEmpty();
    }

    /** The narrow seam insects holds records a refusal through the command, not as a no-op. */
    @Test
    void identificationBudgetSeam_forwardsRefused() {
        NaturalistName pat = NaturalistName.of("pat");

        identificationBudget.refused(pat);

        assertThat(events.findByCounterSince(UsageCounterName.of("identification-refused"), pat, Instant.EPOCH))
                .hasSize(1);
    }
}
