package com.naturalist.usage;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsageTallyTest {

    private static final Observer observer = Observer.forClass(UsageTallyTest.class);

    @Test
    void fullyPopulatedTallyIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedTallyIsValid");
        UsageTally tally = new UsageTally(
                UsageTallyId.create(),
                UsageCounterName.of("identification"),
                NaturalistName.of("patrick-way"),
                "daily-2026-08-25",
                3);

        InvariantObservation result = mo.namedEntity(tally, "tally");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullNaturalistIsValid() {
        // naturalist is nullable-by-design — a global (non-per-user) tally has no owner.
        MethodObserver mo = observer.forMethod("nullNaturalistIsValid");
        UsageTally tally = new UsageTally(
                UsageTallyId.create(),
                UsageCounterName.of("identification"),
                null,
                "daily-2026-08-25",
                7);

        InvariantObservation result = mo.namedEntity(tally, "tally");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        UsageTally tally = new UsageTally(null, null, null, null, 0);

        InvariantObservation result = mo.namedEntity(tally, "tally");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".tally.id",
                        ".tally.counter",
                        ".tally.period");
    }

    @Test
    void withCountReturnsNewInstanceLeavingOriginalUnchanged() {
        UsageTally tally = new UsageTally(
                UsageTallyId.create(),
                UsageCounterName.of("identification"),
                null,
                "daily-2026-08-25",
                1);

        UsageTally updated = tally.withCount(2);

        assertThat(updated.count()).isEqualTo(2);
        assertThat(tally.count()).isEqualTo(1);
    }
}
