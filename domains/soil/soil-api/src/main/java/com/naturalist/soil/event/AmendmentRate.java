package com.naturalist.soil.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * An amendment application rate in pounds per 1000 square feet (lbs/1000 sqft).
 * <p>
 * The FGL reporting standard unit for soil amendment quantities. Scale of 2 reflects
 * FGL reporting precision; amendment rates are not agronomically distinguishable below
 * hundredths of a pound per 1000 sqft.
 * <p>
 * Scale: 2 decimal places. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record AmendmentRate(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static AmendmentRate of(BigDecimal value) {
        return new AmendmentRate(value);
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
