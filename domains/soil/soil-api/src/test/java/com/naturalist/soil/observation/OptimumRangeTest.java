package com.naturalist.soil.observation;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OptimumRangeTest {

    private static final Observer observer = Observer.forClass(OptimumRangeTest.class);

    /** Nitrate-N on CH 2671853-001: {@code 5.3 - 7.2}. */
    @Test
    void closedRangeHasNoViolations() {
        var mo = observer.forMethod("closedRangeHasNoViolations");
        var range = new OptimumRange.Closed(new BigDecimal("5.3"), new BigDecimal("7.2"));

        InvariantObservation result = mo.observable(range, "range");

        assertThat(result.violations()).isEmpty();
    }

    /** Soluble sodium on CH 2671853-001: {@code < 19}, with no floor invented for it. */
    @Test
    void upperBoundedRangeHasNoViolations() {
        var mo = observer.forMethod("upperBoundedRangeHasNoViolations");
        var range = new OptimumRange.UpperBounded(new BigDecimal("19"));

        InvariantObservation result = mo.observable(range, "range");

        assertThat(result.violations()).isEmpty();
    }

    /** {@code ---} on the report. No bounds, nothing to violate. */
    @Test
    void notApplicableHasNoViolations() {
        var mo = observer.forMethod("notApplicableHasNoViolations");

        InvariantObservation result = mo.observable(new OptimumRange.NotApplicable(), "range");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void invertedClosedRangeIsRejected() {
        var mo = observer.forMethod("invertedClosedRangeIsRejected");
        var range = new OptimumRange.Closed(new BigDecimal("7.2"), new BigDecimal("5.3"));

        InvariantObservation result = mo.observable(range, "range");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".range.minNotAboveMax");
    }

    @Test
    void closedRangeMissingBothBoundsProducesOneViolationPerField() {
        var mo = observer.forMethod("closedRangeMissingBothBoundsProducesOneViolationPerField");
        var range = new OptimumRange.Closed(null, null);

        InvariantObservation result = mo.observable(range, "range");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".range.min", ".range.max");
    }

    @Test
    void upperBoundedWithoutACeilingIsRejected() {
        var mo = observer.forMethod("upperBoundedWithoutACeilingIsRejected");

        InvariantObservation result =
                mo.observable(new OptimumRange.UpperBounded(null), "range");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".range.max");
    }
}
