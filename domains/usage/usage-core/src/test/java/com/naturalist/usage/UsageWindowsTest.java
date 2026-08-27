package com.naturalist.usage;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UsageWindowsTest {

    @Test
    void calendarDayWindowStartsAndResetsAtUtcMidnight() {
        var rule = new UsageCounter(UsageCounterId.create(), UsageCounterName.of("identification"),
                UsageScope.GLOBAL, WindowKind.CALENDAR_DAY, null, 50, true);
        var now = Instant.parse("2026-08-25T14:30:00Z");
        assertThat(UsageWindows.windowStart(rule, now)).isEqualTo(Instant.parse("2026-08-25T00:00:00Z"));
        assertThat(UsageWindows.resetAt(rule, now)).isEqualTo(Instant.parse("2026-08-26T00:00:00Z"));
        assertThat(UsageWindows.alertPeriod(rule, now)).isEqualTo("daily-2026-08-25");
    }

    @Test
    void sinceWindowStartsAtConfiguredInstant() {
        var since = Instant.parse("2026-08-01T00:00:00Z");
        var rule = new UsageCounter(UsageCounterId.create(), UsageCounterName.of("identification"),
                UsageScope.GLOBAL, WindowKind.SINCE, since, 650, true);
        var now = Instant.parse("2026-08-25T14:30:00Z");
        assertThat(UsageWindows.windowStart(rule, now)).isEqualTo(since);
        assertThat(UsageWindows.alertPeriod(rule, now)).isEqualTo("monthly-2026-08");
    }
}
