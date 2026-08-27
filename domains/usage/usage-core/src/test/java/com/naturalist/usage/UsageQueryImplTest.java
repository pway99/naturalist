package com.naturalist.usage;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.NaturalistName;
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

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC);

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void reserveStateAndSnapshotCountTodaysEvents() {
        UsageCoreTestContext context = UsageCoreTestContext.create(nte, 80, clock);
        NaturalistName patrickWay = NaturalistName.of("patrick-way");
        Instant now = clock.instant();

        context.events().insert(new UsageEvent(UsageEventId.create(), IDENTIFICATION, patrickWay, now));
        context.events().insert(new UsageEvent(UsageEventId.create(), IDENTIFICATION, patrickWay, now));

        UsageQuery.ReserveState state = context.query().reserveState(IDENTIFICATION, patrickWay, now);
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

        UsageSnapshot snapshot = context.query().snapshot();
        assertThat(snapshot.dailyUsed()).isEqualTo(2);
        assertThat(snapshot.users()).containsExactly(new UsageSnapshot.UserUsage("patrick-way", 2, 10));
    }
}
