package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Soil saturation percentage as reported by the Fruit Growers Laboratory (FGL).
 * <p>
 * The saturation percentage is the gravimetric water content at which the soil paste
 * is visually saturated — used by FGL to characterise soil texture and water-holding
 * capacity. Sandy soils saturate at lower percentages (20–35%); clay soils at higher
 * percentages (50–100%). FGL reports saturation to one decimal place.
 * <p>
 * Scale: 1 decimal place. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record SaturationPct(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static SaturationPct of(BigDecimal value) {
        return new SaturationPct(value);
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
