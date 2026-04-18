package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Soil limestone (CaCO₃) content as a percentage of dry soil weight, as reported
 * by the Fruit Growers Laboratory (FGL).
 * <p>
 * Limestone represents the insoluble calcium carbonate reserve available for
 * Thiobacillus-mediated conversion to plant-available calcium sulfate (gypsum).
 * FGL reports limestone to one decimal place. Oak Vista reference values:
 * Box 1 = 1.7%, Backyard = 2.9% (both in the "Slight Problem" to "Moderate Problem"
 * range on FGL's scale).
 * <p>
 * Scale: 1 decimal place. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record LimestonePct(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static LimestonePct of(BigDecimal value) {
        return new LimestonePct(value);
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

    /**
     * Whether this limestone level exceeds the given threshold (exclusive).
     */
    public boolean isAbove(BigDecimal threshold) {
        return normalized().compareTo(threshold.setScale(scale(), roundingMode())) > 0;
    }
}
