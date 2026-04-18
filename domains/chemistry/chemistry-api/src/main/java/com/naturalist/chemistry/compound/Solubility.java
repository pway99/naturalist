package com.naturalist.chemistry.compound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NumericNamedValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The water solubility of a compound at 20°C, in grams per litre (g/L).
 * <p>
 * Used as the primary solubility measurement on {@link SolubilityProfile}. Scale of 2
 * reflects practical measurement precision — laboratory solubility values are
 * not meaningful below hundredths of a gram per litre for garden-management purposes.
 * <p>
 * Scale: 2 decimal places. Rounding: {@link RoundingMode#HALF_UP}.
 */
public record Solubility(BigDecimal value) implements NumericNamedValue {

    private static final BigDecimal HIGH_SOLUBILITY_THRESHOLD = new BigDecimal("100.00");

    @JsonCreator
    public static Solubility of(BigDecimal value) {
        return new Solubility(value);
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

    /**
     * Whether this solubility exceeds 100 g/L — the threshold for the
     * {@link SolubilityProfile.SolubilityCategory#HIGHLY_SOLUBLE} classification.
     */
    public boolean isHighlySoluble() {
        return value != null && value.compareTo(HIGH_SOLUBILITY_THRESHOLD) > 0;
    }
}
