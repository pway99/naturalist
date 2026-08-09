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
        var saturation = new CationBaseSaturation(
                new BigDecimal("74.6"),
                new BigDecimal("22.9"),
                new BigDecimal("2.09"),
                new BigDecimal("0.408"),
                new BigDecimal("1.00"));

        InvariantObservation result = mo.observable(saturation, "saturation");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponentsProduceOneViolationPerField() {
        var mo = observer.forMethod("nullComponentsProduceOneViolationPerField");
        var saturation = new CationBaseSaturation(null, null, null, null, null);

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
                BigDecimal.ZERO);

        InvariantObservation result = mo.observable(saturation, "saturation");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".saturation.calciumPct");
    }
}
