package com.naturalist.soil.observation;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CationBaseSaturationTest {

    private static final Observer observer = Observer.forClass(CationBaseSaturationTest.class);

    /** Box 1 (CH 2671853-001) — a real, well-formed base-saturation profile. */
    @Test
    void validSaturationHasNoViolations() {
        var mo = observer.forMethod("validSaturationHasNoViolations");
        var saturation = box1();

        InvariantObservation result = mo.observable(saturation, "saturation");

        assertThat(result.violations()).isEmpty();
    }

    /**
     * The reason the censoring marker exists. FGL printed {@code CEC-Hydrogen < 1.00} for Box 1,
     * so the sum is a range, not a number — anything that reports it as exactly 101.4 is claiming
     * a precision the instrument did not deliver.
     */
    @Test
    void censoredHydrogenMakesTheSaturationSumARange() {
        var saturation = box1();

        assertThat(saturation.hasExactSaturationSum()).isFalse();
        assertThat(saturation.saturationSumUpperBound()).isEqualByComparingTo("100.998");
        assertThat(saturation.saturationSumLowerBound()).isEqualByComparingTo("99.998");
    }

    /** Measured hydrogen collapses the range to a point — the distinction Phase 3 is about. */
    @Test
    void measuredHydrogenGivesAnExactSaturationSum() {
        var saturation = new CationBaseSaturation(
                new BigDecimal("74.6"),
                new BigDecimal("22.9"),
                new BigDecimal("2.09"),
                new BigDecimal("0.408"),
                new BigDecimal("1.00"),
                false);

        assertThat(saturation.hasExactSaturationSum()).isTrue();
        assertThat(saturation.saturationSumLowerBound())
                .isEqualByComparingTo(saturation.saturationSumUpperBound());
    }

    @Test
    void nullComponentsProduceOneViolationPerField() {
        var mo = observer.forMethod("nullComponentsProduceOneViolationPerField");
        var saturation = new CationBaseSaturation(null, null, null, null, null, false);

        InvariantObservation result = mo.observable(saturation, "saturation");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".saturation.calciumPct",
                        ".saturation.magnesiumPct",
                        ".saturation.potassiumPct",
                        ".saturation.sodiumPct",
                        ".saturation.hydrogenPct");
    }

    @Test
    void percentageAboveOneHundredIsRejected() {
        var mo = observer.forMethod("percentageAboveOneHundredIsRejected");
        var saturation = new CationBaseSaturation(
                new BigDecimal("150.0"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false);

        InvariantObservation result = mo.observable(saturation, "saturation");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".saturation.calciumPct");
    }

    /** Box 1, CH 2671853-001, March 3 2026 — hydrogen reported as {@code < 1.00}. */
    private static CationBaseSaturation box1() {
        return new CationBaseSaturation(
                new BigDecimal("74.6"),
                new BigDecimal("22.9"),
                new BigDecimal("2.09"),
                new BigDecimal("0.408"),
                new BigDecimal("1.00"),
                true);
    }
}
