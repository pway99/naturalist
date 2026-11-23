package com.naturalist.library;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CladeStepTest {

    private static final Observer observer = Observer.forClass(CladeStepTest.class);

    @Test
    void validStepWithRank() {
        MethodObserver mo = observer.forMethod("validStepWithRank");
        CladeStep step = new CladeStep("animalia", "Animalia", Optional.of(LinealRank.KINGDOM));

        InvariantObservation result = mo.observable(step, "step");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void validStepWithoutRank() {
        MethodObserver mo = observer.forMethod("validStepWithoutRank");
        CladeStep step = new CladeStep("holometabola", "Holometabola", Optional.empty());

        InvariantObservation result = mo.observable(step, "step");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponentsProduceExpectedViolations() {
        MethodObserver mo = observer.forMethod("nullComponentsProduceExpectedViolations");
        CladeStep step = new CladeStep(null, null, null);

        InvariantObservation result = mo.observable(step, "step");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".step.cladeSlug",
                        ".step.displayName",
                        ".step.rank");
    }
}
