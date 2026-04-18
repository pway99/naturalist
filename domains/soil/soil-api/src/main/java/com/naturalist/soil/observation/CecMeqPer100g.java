package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cation exchange capacity in milliequivalents per 100 grams of soil (meq/100g),
 * as reported by the Fruit Growers Laboratory (FGL).
 * <p>
 * CEC measures the soil's capacity to hold positively charged ions (cations) on
 * exchange sites. Higher CEC indicates greater buffering capacity and nutrient
 * retention. FGL reports CEC to one decimal place. Oak Vista reference values:
 * Box 1 ≈ 18.9, Backyard ≈ 24.3 meq/100g (both in the moderate-high range).
 * <p>
 * Scale: 1 decimal place. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record CecMeqPer100g(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static CecMeqPer100g of(BigDecimal value) {
        return new CecMeqPer100g(value);
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
