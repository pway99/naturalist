package com.naturalist.soil;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The pH of a soil sample as reported by the Fruit Growers Laboratory (FGL).
 * <p>
 * Scale of 2 reflects FGL reporting precision — pH values are reported to two
 * decimal places. {@code 7.20} is distinct from {@code 7.2} in the FGL report format;
 * normalised storage preserves this precision without silent rounding.
 * <p>
 * Valid range is 0–14 (log scale). Oak Vista reference: Box 1 at pH 7.2 (OPTIMAL
 * for the mixed raised-bed substrate; no liming indicated).
 * <p>
 * Scale: 2 decimal places. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record SoilPH(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static SoilPH of(BigDecimal value) {
        return new SoilPH(value);
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
        return value != null
                && value.compareTo(BigDecimal.ZERO) >= 0
                && value.compareTo(new BigDecimal("14")) <= 0;
    }
}
