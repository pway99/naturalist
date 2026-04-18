package com.naturalist.measurements;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Soil volumetric moisture as a percentage (0–100%), measured or derived by soil sensors.
 * <p>
 * Volumetric moisture is the fraction of soil volume occupied by water. Sensor readings
 * are typically derived from capacitance or impedance measurement and may be calibrated
 * per substrate. Scale of 1 decimal place reflects typical sensor reporting precision
 * and agronomic significance for irrigation management.
 * <p>
 * Scale: 1 decimal place. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record MoisturePercent(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static MoisturePercent of(BigDecimal value) {
        return new MoisturePercent(value);
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
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0;
    }
}
