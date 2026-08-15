package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Sodium Adsorption Ratio (SAR) — sodium measured against calcium and magnesium on the exchange
 * complex, the standard index of sodium hazard to soil structure. Dimensionless.
 * <p>
 * SAR answers a question EC cannot. EC says how much salt is present; SAR says whether that salt
 * is the kind that disperses clay and destroys structure. A soil can carry a harmless total salt
 * load and still be sodic. FGL reports both, and the two belong side by side.
 * <p>
 * <b>Oak Vista reference (March 3, 2026):</b> Box 1 SAR 0.3, back yard 0.4 — far below the
 * conventional sodicity threshold of 13, and consistent with the negligible sodium base
 * saturation both samples show. Not a problem here; recorded because it is the measurement that
 * would show the problem arriving, particularly in the clay-sublayer back yard.
 * <p>
 * Scale: 1 decimal place. Rounding: {@link RoundingMode#HALF_UP}. Zero is a legitimate value —
 * a soil with no measurable sodium has a SAR of zero.
 */
public record SodiumAdsorptionRatio(BigDecimal value) implements NumericNamedValue {

    @JsonCreator
    public static SodiumAdsorptionRatio of(BigDecimal value) {
        return new SodiumAdsorptionRatio(value);
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
