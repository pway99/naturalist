package com.naturalist.usage;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.TestEntitySourceTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class UsageCounterTestEntitySourceTest
        extends TestEntitySourceTest<UsageCounterId, UsageCounter, UsageCounterTestEntitySource> {

    /**
     * Three rules are seeded — see {@code usage/usage-counters.json}.
     */
    @Override
    protected int minimumEntities() {
        return 3;
    }

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void seededPerUserDailyRuleLoads() {
        UsageCounterTestEntitySource source = nte.getNamed(UsageCounterTestEntitySource.class);

        assertThat(source.getByName(TestUsageIdentifiers.UsageCounters.PerUserDailyId))
                .contains(new UsageCounter(
                        TestUsageIdentifiers.UsageCounters.PerUserDailyId,
                        TestUsageIdentifiers.UsageCounters.Identification,
                        UsageScope.PER_USER,
                        WindowKind.CALENDAR_DAY,
                        null,
                        10,
                        true));
    }

    @Test
    void seededGlobalMonthlyRuleLoadsWithSinceWindow() {
        UsageCounterTestEntitySource source = nte.getNamed(UsageCounterTestEntitySource.class);

        UsageCounter monthly = source.getByName(TestUsageIdentifiers.UsageCounters.GlobalMonthlyId).orElseThrow();

        assertThat(monthly.windowKind()).isEqualTo(WindowKind.SINCE);
        assertThat(monthly.since()).isNotNull();
        assertThat(monthly.limit()).isEqualTo(650);
    }
}
