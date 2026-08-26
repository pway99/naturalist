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

class UsageBudgetServiceTest {

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC);

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void per_user_daily_quota_blocks_after_limit() {
        UsageCoreTestContext context = UsageCoreTestContext.create(
                nte, new UsageLimits(2, 9999, 9999, 9999, 80), clock);
        UsageBudgetService service = context.service();
        NaturalistName pat = NaturalistName.of("pat");

        service.reserve(pat);
        service.reserve(pat);

        assertThatThrownBy(() -> service.reserve(pat))
                .isInstanceOf(BudgetExceededException.class)
                .extracting(ex -> ((BudgetExceededException) ex).limitKind())
                .isEqualTo(LimitKind.PER_USER);
    }

    @Test
    void global_daily_cap_blocks_across_users() {
        UsageCoreTestContext context = UsageCoreTestContext.create(
                nte, new UsageLimits(9999, 9999, 2, 9999, 80), clock);
        UsageBudgetService service = context.service();

        service.reserve(NaturalistName.of("naturalist-a"));
        service.reserve(NaturalistName.of("naturalist-b"));

        assertThatThrownBy(() -> service.reserve(NaturalistName.of("naturalist-c")))
                .isInstanceOf(BudgetExceededException.class)
                .extracting(ex -> ((BudgetExceededException) ex).limitKind())
                .isEqualTo(LimitKind.DAILY);
    }

    @Test
    void rejected_reserve_does_not_consume_other_counters() {
        UsageCoreTestContext context = UsageCoreTestContext.create(
                nte, new UsageLimits(1, 9999, 9999, 9999, 80), clock);
        UsageBudgetService service = context.service();
        NaturalistName pat = NaturalistName.of("pat");

        service.reserve(pat);
        assertThatThrownBy(() -> service.reserve(pat)).isInstanceOf(BudgetExceededException.class);

        assertThat(service.snapshot().dailyUsed()).isEqualTo(1);
    }

    @Test
    void warning_and_hard_stop_alerts_recorded_once() {
        UsageCoreTestContext context = UsageCoreTestContext.create(
                nte, new UsageLimits(9999, 9999, 9999, 5, 80), clock);
        UsageBudgetService service = context.service();

        for (int i = 0; i < 5; i++) {
            service.reserve(NaturalistName.of("monthly-user-" + i));
        }
        for (int i = 0; i < 3; i++) {
            NaturalistName extra = NaturalistName.of("monthly-extra-" + i);
            assertThatThrownBy(() -> service.reserve(extra))
                    .isInstanceOf(BudgetExceededException.class);
        }

        List<UsageAlert> alerts = context.alertRepository().getUnacknowledged();
        assertThat(alerts).filteredOn(a -> a.kind() == AlertKind.WARNING).hasSize(1);
        assertThat(alerts).filteredOn(a -> a.kind() == AlertKind.HARD_STOP).hasSize(1);
    }

    @Test
    void concurrent_reserves_do_not_overshoot() throws InterruptedException {
        UsageCoreTestContext context = UsageCoreTestContext.create(
                nte, new UsageLimits(99, 9999, 100, 9999, 80), clock);
        UsageBudgetService service = context.service();

        int threadCount = 16;
        int totalAttempts = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        AtomicInteger succeeded = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(totalAttempts);

        for (int i = 0; i < totalAttempts; i++) {
            NaturalistName naturalist = NaturalistName.of("concurrent-user-" + i);
            pool.submit(() -> {
                try {
                    service.reserve(naturalist);
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
