package com.naturalist.measurements;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A precipitation accumulation in inches, as recorded by a weather station or manual gauge.
 * <p>
 * Scale of 2 reflects weather station measurement precision. Values below 0.01 inches
 * are not agronomically significant for leaching or soil moisture calculations at the
 * Oak Vista property scale.
 * <p>
 * Scale: 2 decimal places. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record PrecipitationInches(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static PrecipitationInches of(BigDecimal value) {
        return new PrecipitationInches(value);
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
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Whether this accumulation meets or exceeds the given threshold.
     */
    public boolean isAtLeast(BigDecimal thresholdInches) {
        return value.compareTo(thresholdInches) >= 0;
    }
}
