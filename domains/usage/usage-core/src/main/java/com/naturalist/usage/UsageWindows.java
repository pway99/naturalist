package com.naturalist.usage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * Pure window-math helper for {@link UsageCounter} rules — replaces the old
 * period-slug computation ({@code UsagePeriods}) now that usage is metered by
 * counting {@link UsageEvent} rows rather than upserting tally rows per period.
 * Every method takes the rule plus a {@code now} instant captured once per
 * {@code reserve}/{@code reserveState}/{@code snapshot} invocation, so a single
 * call observes one consistent window rather than re-deriving day/month at
 * each use.
 *
 * <p><b>Callers must supply a UTC-derived {@code now}.</b> {@link #windowStart}
 * and {@link #resetAt} anchor {@code CALENDAR_DAY} boundaries to
 * {@link ZoneOffset#UTC} via {@link LocalDate#ofInstant(Instant, java.time.ZoneId)};
 * a non-UTC clock would shift which wall-clock moment a window starts or resets
 * at. The production bean supplies {@code Clock.systemUTC()}.
 */
final class UsageWindows {

    private UsageWindows() {
    }

    /** The start of the rule's current window: UTC midnight for {@code CALENDAR_DAY}, {@code rule.since()} for {@code SINCE}. */
    static Instant windowStart(UsageCounter rule, Instant now) {
        return switch (rule.windowKind()) {
            case CALENDAR_DAY -> utcDate(now).atStartOfDay(ZoneOffset.UTC).toInstant();
            case SINCE -> rule.since();
        };
    }

    /**
     * When the rule's window next resets — next UTC midnight for {@code CALENDAR_DAY};
     * for {@code SINCE} this is nominal/display-only (the window itself never actually
     * resets), reported as the start of next UTC month.
     */
    static Instant resetAt(UsageCounter rule, Instant now) {
        return switch (rule.windowKind()) {
            case CALENDAR_DAY -> utcDate(now).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            case SINCE -> utcYearMonth(now).plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        };
    }

    static AlertScope alertScope(UsageCounter rule) {
        return switch (rule.windowKind()) {
            case CALENDAR_DAY -> AlertScope.DAILY;
            case SINCE -> AlertScope.MONTHLY;
        };
    }

    static String alertPeriod(UsageCounter rule, Instant now) {
        return switch (alertScope(rule)) {
            case DAILY -> "daily-" + utcDate(now);
            case MONTHLY -> "monthly-" + utcYearMonth(now);
        };
    }

    private static LocalDate utcDate(Instant now) {
        return LocalDate.ofInstant(now, ZoneOffset.UTC);
    }

    private static YearMonth utcYearMonth(Instant now) {
        return YearMonth.from(utcDate(now));
    }
}
