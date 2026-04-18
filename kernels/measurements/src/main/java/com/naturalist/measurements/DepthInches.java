package com.naturalist.measurements;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A depth measurement in inches, used across domains for spatial measurements.
 * <p>
 * Scale of 2 reflects practical installation and measurement precision — soil management
 * depths are not meaningful below hundredths of an inch for any agronomic calculation
 * in this application. Used for sensor installation depth, mulch layer depth, and
 * tillage depth.
 * <p>
 * Scale: 2 decimal places. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record DepthInches(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static DepthInches of(BigDecimal value) {
        return new DepthInches(value);
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

    /**
     * Whether this depth is at or below the given threshold (inclusive).
     */
    public boolean isAtMost(BigDecimal thresholdInches) {
        return value.compareTo(thresholdInches) <= 0;
    }
}
