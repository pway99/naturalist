package com.naturalist.measurements;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Soil electrical conductivity in deci-Siemens per metre (dS/m), as reported by
 * the Fruit Growers Laboratory (FGL) and measured by soil sensors.
 * <p>
 * EC is the primary measure of soluble salt concentration in the soil solution.
 * Elevated EC suppresses water uptake by reducing osmotic potential. Scale of 2
 * reflects FGL reporting standard and sensor measurement precision; readings below
 * 0.01 dS/m are not agronomically significant and are treated as zero.
 * <p>
 * Scale: 2 decimal places. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record ElectricalConductivity(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static ElectricalConductivity of(BigDecimal value) {
        return new ElectricalConductivity(value);
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
}
