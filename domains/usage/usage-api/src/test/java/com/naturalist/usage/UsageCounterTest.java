package com.naturalist.usage;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsageCounterTest {

    private static final Observer observer = Observer.forClass(UsageCounterTest.class);

    @Test
    void fullyPopulatedCounterIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedCounterIsValid");
        UsageCounter counter = new UsageCounter(UsageCounterName.of("identification"));

        InvariantObservation result = mo.namedEntity(counter, "counter");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullNameReportsViolation() {
        MethodObserver mo = observer.forMethod("nullNameReportsViolation");
        UsageCounter counter = new UsageCounter(null);

        InvariantObservation result = mo.namedEntity(counter, "counter");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".counter.name");
    }
}
