package com.naturalist.usage;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UsageAlertTest {

    private static final Observer observer = Observer.forClass(UsageAlertTest.class);

    @Test
    void fullyPopulatedAlertIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedAlertIsValid");
        UsageAlert alert = new UsageAlert(
                UsageAlertId.create(),
                UsageCounterName.of("identification"),
                AlertScope.DAILY,
                AlertKind.WARNING,
                "daily-2026-08-25",
                "daily identification budget warning: 40/50 used",
                Instant.parse("2026-08-25T15:00:00Z"),
                false,
                false);

        InvariantObservation result = mo.namedEntity(alert, "alert");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        UsageAlert alert = new UsageAlert(null, null, null, null, null, null, null, false, false);

        InvariantObservation result = mo.namedEntity(alert, "alert");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".alert.id",
                        ".alert.counter",
                        ".alert.scope",
                        ".alert.kind",
                        ".alert.period",
                        ".alert.message",
                        ".alert.at");
    }

    @Test
    void withEmailedReturnsNewInstanceLeavingOriginalUnchanged() {
        UsageAlert alert = new UsageAlert(
                UsageAlertId.create(),
                UsageCounterName.of("identification"),
                AlertScope.MONTHLY,
                AlertKind.HARD_STOP,
                "monthly-2026-08",
                "monthly identification budget hard_stop: 650/650 used",
                Instant.parse("2026-08-25T15:00:00Z"),
                false,
                false);

        UsageAlert updated = alert.withEmailed(true);

        assertThat(updated.emailed()).isTrue();
        assertThat(alert.emailed()).isFalse();
    }

    @Test
    void withAcknowledgedReturnsNewInstanceLeavingOriginalUnchanged() {
        UsageAlert alert = new UsageAlert(
                UsageAlertId.create(),
                UsageCounterName.of("identification"),
                AlertScope.MONTHLY,
                AlertKind.HARD_STOP,
                "monthly-2026-08",
                "monthly identification budget hard_stop: 650/650 used",
                Instant.parse("2026-08-25T15:00:00Z"),
                false,
                false);

        UsageAlert updated = alert.withAcknowledged(true);

        assertThat(updated.acknowledged()).isTrue();
        assertThat(alert.acknowledged()).isFalse();
    }
}
