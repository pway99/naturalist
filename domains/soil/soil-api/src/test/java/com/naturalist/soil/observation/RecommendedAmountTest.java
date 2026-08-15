package com.naturalist.soil.observation;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendedAmountTest {

    private static final Observer observer = Observer.forClass(RecommendedAmountTest.class);

    /** Box 1's potassium row: {@code 11.2 Lbs/1000 SqFt}. */
    @Test
    void quantityHasNoViolations() {
        var mo = observer.forMethod("quantityHasNoViolations");

        InvariantObservation result = mo.observable(
                new RecommendedAmount.Quantity(new BigDecimal("11.2")), "amount");

        assertThat(result.violations()).isEmpty();
    }

    /** Box 1's Lime Requirement: {@code 0 Tons/AF}. A computed zero is a real answer. */
    @Test
    void quantityOfZeroHasNoViolations() {
        var mo = observer.forMethod("quantityOfZeroHasNoViolations");

        InvariantObservation result =
                mo.observable(new RecommendedAmount.Quantity(BigDecimal.ZERO), "amount");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void noneHasNoViolations() {
        var mo = observer.forMethod("noneHasNoViolations");

        InvariantObservation result = mo.observable(new RecommendedAmount.None(), "amount");

        assertThat(result.violations()).isEmpty();
    }

    /** Both reports' Gypsum Requirement: {@code < 0.50 Tons/AF}. */
    @Test
    void belowDetectionLimitHasNoViolations() {
        var mo = observer.forMethod("belowDetectionLimitHasNoViolations");

        InvariantObservation result = mo.observable(
                new RecommendedAmount.BelowDetectionLimit(new BigDecimal("0.50")), "amount");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void negativeQuantityIsRejected() {
        var mo = observer.forMethod("negativeQuantityIsRejected");

        InvariantObservation result = mo.observable(
                new RecommendedAmount.Quantity(new BigDecimal("-1")), "amount");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".amount.notNegative");
    }

    /**
     * {@code < 0} is not a statement about anything, and {@code < 0.00} would be a roundabout way
     * of saying zero. A censoring bound has to be positive to carry information.
     */
    @Test
    void nonPositiveDetectionLimitIsRejected() {
        var mo = observer.forMethod("nonPositiveDetectionLimitIsRejected");

        InvariantObservation result = mo.observable(
                new RecommendedAmount.BelowDetectionLimit(BigDecimal.ZERO), "amount");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".amount.positiveLimit");
    }

    @Test
    void missingValuesProduceAViolation() {
        var mo = observer.forMethod("missingValuesProduceAViolation");

        assertThat(mo.observable(new RecommendedAmount.Quantity(null), "amount")
                .violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".amount.value");
    }
}
