package com.naturalist.usage;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Pure period-slug + reset-instant computation shared by {@link UsageQueryImpl}
 * (needs the slugs to shape a batched tally read) and {@link UsageCommandImpl}
 * (needs both the slugs and the reset instants for {@link BudgetExceededException}).
 * Captured once per call from the injected {@link Clock} via {@link #now(Clock)} so a
 * single {@code reserve}/{@code reserveState}/{@code snapshot} invocation observes
 * one consistent instant rather than re-deriving day/month/minute at each use.
 *
 * <p><b>The {@link Clock} is expected to be UTC-zoned.</b> Period buckets
 * ({@code dailySlug}, {@code monthlySlug}, {@code rateSlug}) are derived via
 * {@code LocalDate}/{@code YearMonth}/{@code LocalDateTime} {@code .now(clock)}, and
 * each {@code startOfNext*} reset instant is then computed by anchoring those values
 * to {@link ZoneOffset#UTC}. A non-UTC clock would shift which wall-clock moment a
 * bucket rolls over at, desynchronizing the reset boundary from the bucket it was
 * derived from. The production bean supplies {@code Clock.systemUTC()}.
 */
final class UsagePeriods {

    private static final DateTimeFormatter RATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");

    private final LocalDate day;
    private final YearMonth month;
    private final LocalDateTime minute;

    final String dailySlug;
    final String monthlySlug;
    final String rateSlug;

    private UsagePeriods(LocalDate day, YearMonth month, LocalDateTime minute) {
        this.day = day;
        this.month = month;
        this.minute = minute;
        this.dailySlug = "daily-" + day;
        this.monthlySlug = "monthly-" + month;
        this.rateSlug = "rate-" + minute.format(RATE_FORMAT);
    }

    static UsagePeriods now(Clock clock) {
        return new UsagePeriods(
                LocalDate.now(clock),
                YearMonth.now(clock),
                LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES));
    }

    Instant startOfNextMonth() {
        return month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    Instant startOfNextDay() {
        return day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    Instant startOfNextMinute() {
        return minute.plusMinutes(1).atZone(ZoneOffset.UTC).toInstant();
    }
}
