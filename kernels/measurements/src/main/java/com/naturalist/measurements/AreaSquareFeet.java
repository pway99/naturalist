package com.naturalist.measurements;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A spatial area measurement in square feet, used for zone and sub-zone boundaries.
 * <p>
 * Scale of 1 reflects practical measurement precision for property-scale boundaries.
 * Sub-unit decimal places (tenth of a square foot) are meaningful for small garden beds
 * but not for larger property zones.
 * <p>
 * Scale: 1 decimal place. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record AreaSquareFeet(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static AreaSquareFeet of(BigDecimal value) {
        return new AreaSquareFeet(value);
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
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
