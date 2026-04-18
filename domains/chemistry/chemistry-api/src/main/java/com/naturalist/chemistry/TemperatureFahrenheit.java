package com.naturalist.chemistry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A temperature reading in degrees Fahrenheit.
 * <p>
 * Used across chemistry domain profiles (volatilization, safety, reaction conditions)
 * wherever a temperature threshold or measurement is required. Scale of 1 reflects
 * sensor precision — tenths of a degree are the finest distinction this application
 * reasons about.
 * <p>
 * Scale: 1 decimal place. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record TemperatureFahrenheit(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static TemperatureFahrenheit of(BigDecimal value) {
        return new TemperatureFahrenheit(value);
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
        return value != null;
    }

    /**
     * Whether this temperature is strictly below {@code other}.
     */
    public boolean isBelow(TemperatureFahrenheit other) {
        return value.compareTo(other.value) < 0;
    }

    /**
     * Whether this temperature is strictly above {@code other}.
     */
    public boolean isAbove(TemperatureFahrenheit other) {
        return value.compareTo(other.value) > 0;
    }

    /**
     * Whether this temperature is within {@code delta} degrees of {@code other}.
     */
    public boolean isWithin(BigDecimal delta, TemperatureFahrenheit other) {
        return value.subtract(other.value).abs().compareTo(delta) <= 0;
    }
}
