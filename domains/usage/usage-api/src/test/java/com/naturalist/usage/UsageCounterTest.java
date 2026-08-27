package com.naturalist.usage;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UsageCounterTest {

    private static final Observer observer = Observer.forClass(UsageCounterTest.class);

    @Test
    void fullyPopulatedCalendarDayCounterIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedCalendarDayCounterIsValid");
        UsageCounter counter = new UsageCounter(
                UsageCounterId.create(),
                UsageCounterName.of("identification"),
                UsageScope.PER_USER,
                WindowKind.CALENDAR_DAY,
                null,
                10,
                true);

        InvariantObservation result = mo.namedEntity(counter, "counter");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void fullyPopulatedSinceCounterIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedSinceCounterIsValid");
        UsageCounter counter = new UsageCounter(
                UsageCounterId.create(),
                UsageCounterName.of("identification"),
                UsageScope.GLOBAL,
                WindowKind.SINCE,
                Instant.parse("2026-08-01T00:00:00Z"),
                650,
                true);

        InvariantObservation result = mo.namedEntity(counter, "counter");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void sinceWindowWithNullSinceReportsViolation() {
        MethodObserver mo = observer.forMethod("sinceWindowWithNullSinceReportsViolation");
        UsageCounter counter = new UsageCounter(
                UsageCounterId.create(),
                UsageCounterName.of("identification"),
                UsageScope.GLOBAL,
                WindowKind.SINCE,
                null,
                650,
                true);

        InvariantObservation result = mo.namedEntity(counter, "counter");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".counter.since");
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        UsageCounter counter = new UsageCounter(null, null, null, null, null, 0, false);

        InvariantObservation result = mo.namedEntity(counter, "counter");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".counter.id",
                        ".counter.counterName",
                        ".counter.scope",
                        ".counter.windowKind");
    }

    @Test
    void negativeLimitReportsViolation() {
        MethodObserver mo = observer.forMethod("negativeLimitReportsViolation");
        UsageCounter counter = new UsageCounter(
                UsageCounterId.create(),
                UsageCounterName.of("identification"),
                UsageScope.PER_USER,
                WindowKind.CALENDAR_DAY,
                null,
                -1,
                true);

        InvariantObservation result = mo.namedEntity(counter, "counter");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".counter.limit");
    }

    @Test
    void withLimitReturnsNewInstanceLeavingOriginalUnchanged() {
        UsageCounter counter = new UsageCounter(
                UsageCounterId.create(),
                UsageCounterName.of("identification"),
                UsageScope.PER_USER,
                WindowKind.CALENDAR_DAY,
                null,
                10,
                true);

        UsageCounter updated = counter.withLimit(20);

        assertThat(updated.limit()).isEqualTo(20);
        assertThat(counter.limit()).isEqualTo(10);
    }

    @Test
    void withActiveReturnsNewInstanceLeavingOriginalUnchanged() {
        UsageCounter counter = new UsageCounter(
                UsageCounterId.create(),
                UsageCounterName.of("identification"),
                UsageScope.PER_USER,
                WindowKind.CALENDAR_DAY,
                null,
                10,
                true);

        UsageCounter updated = counter.withActive(false);

        assertThat(updated.active()).isFalse();
        assertThat(counter.active()).isTrue();
    }

    @Test
    void withSinceReturnsNewInstanceLeavingOriginalUnchanged() {
        UsageCounter counter = new UsageCounter(
                UsageCounterId.create(),
                UsageCounterName.of("identification"),
                UsageScope.GLOBAL,
                WindowKind.SINCE,
                Instant.parse("2026-08-01T00:00:00Z"),
                650,
                true);

        UsageCounter updated = counter.withSince(Instant.parse("2026-09-01T00:00:00Z"));

        assertThat(updated.since()).isEqualTo(Instant.parse("2026-09-01T00:00:00Z"));
        assertThat(counter.since()).isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
    }
}
