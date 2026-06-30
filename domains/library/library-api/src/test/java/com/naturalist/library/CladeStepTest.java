package com.naturalist.library;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CladeStepTest {

    private static final Observer observer = Observer.forClass(CladeStepTest.class);

    @Test
    void validStepWithRank() {
        var mo = observer.forMethod("validStepWithRank");
        var step = new CladeStep("animalia", "Animalia", Optional.of(LinealRank.KINGDOM));

        InvariantObservation result = mo.observable(step, "step");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void validStepWithoutRank() {
        var mo = observer.forMethod("validStepWithoutRank");
        var step = new CladeStep("holometabola", "Holometabola", Optional.empty());

        InvariantObservation result = mo.observable(step, "step");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponentsProduceExpectedViolations() {
        var mo = observer.forMethod("nullComponentsProduceExpectedViolations");
        var step = new CladeStep(null, null, null);

        InvariantObservation result = mo.observable(step, "step");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".step.cladeSlug",
                        ".step.displayName",
                        ".step.rank");
    }
}
