package com.naturalist.usage;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.test.context.Fake;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link UsageQueryImpl} directly — the read half of what used to be a
 * single combined {@code UsageBudgetService}. {@link UsageCommandImplTest} covers
 * the write half ({@link UsageCommandImpl#reserve}); this class pins the counting
 * behaviour ({@link UsageQueryImpl#reserveState} / {@link UsageQueryImpl#snapshot()})
 * against a raw event log rather than through a reservation.
 */
class UsageQueryImplTest {

    private static final UsageCounterName IDENTIFICATION = UsageCounterName.of("identification");

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Fake
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC);

    @Wired
    UsageQuery query;

    @Wired
    UsageRepository.EventRepository events;

    @Test
    void reserveStateAndSnapshotCountTodaysEvents() {
        NaturalistName patrickWay = NaturalistName.of("patrick-way");
        Instant now = clock.instant();

        events.insert(new UsageEvent(UsageEventId.create(), IDENTIFICATION, patrickWay, now));
        events.insert(new UsageEvent(UsageEventId.create(), IDENTIFICATION, patrickWay, now));

        UsageQuery.ReserveState state = query.reserveState(IDENTIFICATION, patrickWay, now);
        assertThat(state.counters())
                .anySatisfy(cu -> {
                    assertThat(cu.rule().scope()).isEqualTo(UsageScope.PER_USER);
                    assertThat(cu.rule().windowKind()).isEqualTo(WindowKind.CALENDAR_DAY);
                    assertThat(cu.used()).isEqualTo(2);
                })
                .anySatisfy(cu -> {
                    assertThat(cu.rule().scope()).isEqualTo(UsageScope.GLOBAL);
                    assertThat(cu.rule().windowKind()).isEqualTo(WindowKind.CALENDAR_DAY);
                    assertThat(cu.used()).isEqualTo(2);
                });

        UsageSnapshot snapshot = query.snapshot();
        assertThat(snapshot.dailyUsed()).isEqualTo(2);
        assertThat(snapshot.users()).containsExactly(new UsageSnapshot.UserUsage("patrick-way", 2, 10));
    }
}
