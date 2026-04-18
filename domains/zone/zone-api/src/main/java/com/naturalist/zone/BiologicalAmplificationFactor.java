package com.naturalist.zone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A substrate-dependent multiplier for extending soil recovery timelines after physical disturbance.
 * <p>
 * The biological amplification factor modifies baseline drain recovery times in response to
 * substrate-specific characteristics. Native clay sublayers, silt content, and organic matter
 * density affect how quickly biological activity re-establishes soil structure post-tillage.
 * <p>
 * Typical range: 0.70 (fast recovery) to 1.50 (slow recovery). Scale of 2 supports fine-grained
 * distinction between similar substrates.
 * <p>
 * Scale: 2 decimal places. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record BiologicalAmplificationFactor(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static BiologicalAmplificationFactor of(BigDecimal value) {
        return new BiologicalAmplificationFactor(value);
    }

    @Override
    public int scale() {
        return 2;
    }

    @Override
    public RoundingMode roundingMode() {
        return RoundingMode.HALF_UP;
    }

    @Override
    public boolean isValid() {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
