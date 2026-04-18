package com.naturalist.measurements;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A slope angle in degrees (0–90°), used to describe zone aspect and terrain orientation.
 * <p>
 * Scale of 1 reflects practical measurement precision for hand-measured slopes.
 * Values are constrained to the range [0, 90] degrees, where 0° is level terrain
 * and 90° is vertical. Fractional degrees distinguish subtle variations in water
 * runoff and sun exposure.
 * <p>
 * Scale: 1 decimal place. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record SlopeDegrees(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static SlopeDegrees of(BigDecimal value) {
        return new SlopeDegrees(value);
    }

    @Override
    public int scale() {
        return 1;
    }

    @Override
    public RoundingMode roundingMode() {
        return RoundingMode.HALF_UP;
    }

    @Override
    public boolean isValid() {
        return value != null
                && value.compareTo(BigDecimal.ZERO) >= 0
                && value.compareTo(new BigDecimal("90")) <= 0;
    }
}
